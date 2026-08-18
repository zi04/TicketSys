package com.ticketflow;

import com.ticketflow.domain.Event;
import com.ticketflow.domain.Event.EventStatus;
import com.ticketflow.domain.InvariantViolationException;
import com.ticketflow.domain.Money;
import com.ticketflow.domain.Order;
import com.ticketflow.domain.Reservation;
import com.ticketflow.domain.ReservationLine;
import com.ticketflow.domain.TicketType;
import com.ticketflow.domain.id.CustomerId;
import com.ticketflow.domain.id.EventId;
import com.ticketflow.domain.id.OrderId;
import com.ticketflow.domain.id.OrganiserId;
import com.ticketflow.domain.id.ReservationId;
import com.ticketflow.failure.Failure;
import com.ticketflow.storage.EventRepository;
import com.ticketflow.storage.OrderRepository;
import com.ticketflow.storage.ReservationRepository;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.control.Either;

import java.time.Instant;
import java.util.function.Supplier;

public final class TicketFlowManager {

    private final EventRepository events;
    private final ReservationRepository reservations;
    private final OrderRepository orders;
    private final Supplier<Instant> currentTime;

    public TicketFlowManager(EventRepository events, ReservationRepository reservations,
                             OrderRepository orders) {
        this(events, reservations, orders, Instant::now);
    }

    TicketFlowManager(EventRepository events, ReservationRepository reservations,
                      OrderRepository orders, Supplier<Instant> currentTime) {
        this.events = events;
        this.reservations = reservations;
        this.orders = orders;
        this.currentTime = currentTime;
    }

    public Either<Failure, Event> createEvent(OrganiserId organiserId, String name,
                                               Instant salesOpenAt, Instant salesCloseAt, Instant startsAt) {
        if (name == null || name.isBlank()) {
            return Either.left(new Failure.ValidationError("event name must not be blank"));
        }
        if (salesOpenAt == null || salesCloseAt == null || startsAt == null) {
            return Either.left(new Failure.ValidationError("event times must not be null"));
        }
        if (salesOpenAt.isAfter(salesCloseAt) || salesCloseAt.isAfter(startsAt)) {
            return Either.left(new Failure.ValidationError("sales window must satisfy opens <= closes <= starts"));
        }

        Event event = Event.draft(organiserId, name, salesOpenAt, salesCloseAt, startsAt);
        return Either.right(events.save(event));
    }

    public Either<Failure, Event> addTicketType(EventId eventId, OrganiserId organiserId,
                                                 String name, Money price, int allocation,
                                                 int perCustomerLimit) {
        return findEvent(eventId)
                .flatMap(event -> requireEventOwner(event, organiserId))
                .flatMap(this::requireDraft)
                .map(event -> {
                    TicketType ticket = TicketType.create(
                            event.getId(), name, price, allocation, perCustomerLimit);
                    return events.save(event.withTicketTypeAdded(ticket));
                });
    }

    public Either<Failure, Event> publish(EventId eventId, OrganiserId organiserId) {
        return findEvent(eventId)
                .flatMap(event -> requireEventOwner(event, organiserId))
                .flatMap(this::requireTicketTypes)
                .flatMap(this::requireEventNotStarted)
                .map(event -> events.save(event.withStatus(EventStatus.PUBLISHED)));
    }

    public Either<Failure, Event> get(EventId eventId) {
        return findEvent(eventId);
    }

    public List<Event> listAll() {
        return events.findAll();
    }

    public Either<Failure, Reservation> reserve(EventId eventId, CustomerId customerId,
                                                 List<ReservationRequestLine> request) {
        if (request == null || request.isEmpty()) {
            return Either.left(new Failure.EmptyReservationRequest());
        }
        Instant now = currentTime.get();

        return findEvent(eventId)
                .flatMap(event -> requireOnSale(event, now))
                .flatMap(event -> checkAllLines(event, customerId, request, now))
                .map(result -> {
                    Event updatedEvent = result._1;
                    List<ReservationLine> lines = result._2;
                    events.save(updatedEvent);
                    Reservation reservation = Reservation.hold(eventId, customerId, lines, now);
                    return reservations.save(reservation);
                });
    }

    public Either<Failure, Reservation> cancel(ReservationId reservationId, CustomerId customerId) {
        Instant now = currentTime.get();
        return findReservation(reservationId)
                .flatMap(reservation -> requireCustomer(reservation, customerId))
                .flatMap(reservation -> requireActionable(reservation, now))
                .flatMap(reservation -> releaseSeats(reservation).map(ignored -> reservation))
                .map(reservation -> reservations.save(reservation.cancel()));
    }

    public List<Reservation> expireDue() {
        Instant now = currentTime.get();
        return reservations.findExpirable(now)
                .map(reservation -> expireIfNeeded(reservation, now));
    }

    public Either<Failure, Reservation> get(ReservationId reservationId) {
        return findReservation(reservationId);
    }

    public Either<Failure, Order> pay(ReservationId reservationId, CustomerId customerId,
                                      Money amountGiven, String paymentReference) {
        if (paymentReference == null || paymentReference.isBlank()) {
            return Either.left(new Failure.ValidationError("a payment reference is required"));
        }

        var oldOrder = orders.findByPaymentReference(paymentReference);
        if (oldOrder.isDefined()) {
            return Either.right(oldOrder.get());
        }

        Instant now = currentTime.get();
        return findReservation(reservationId)
                .flatMap(reservation -> requireCustomer(reservation, customerId))
                .flatMap(reservation -> requireActionable(reservation, now))
                .flatMap(reservation -> requireExactAmount(reservation, amountGiven))
                .flatMap(this::sellReservedSeats)
                .map(reservation -> {
                    Reservation confirmed = reservations.save(reservation.confirm());
                    return orders.save(Order.from(confirmed, paymentReference, now));
                });
    }

    public Either<Failure, Order> getOrder(OrderId orderId) {
        return orders.findById(orderId)
                .toEither((Failure) new Failure.NotFound("order", orderId.toString()));
    }

    private Either<Failure, Event> findEvent(EventId eventId) {
        return events.findById(eventId)
                .toEither((Failure) new Failure.NotFound("event", eventId.toString()));
    }

    private Either<Failure, Reservation> findReservation(ReservationId reservationId) {
        return reservations.findById(reservationId)
                .toEither((Failure) new Failure.NotFound("reservation", reservationId.toString()));
    }

    private Either<Failure, Event> requireEventOwner(Event event, OrganiserId organiserId) {
        return event.isOwnedBy(organiserId)
                ? Either.right(event)
                : Either.left(new Failure.NotEventOwner(event.getName()));
    }

    private Either<Failure, Event> requireDraft(Event event) {
        return event.getStatus() == EventStatus.DRAFT
                ? Either.right(event)
                : Either.left(new Failure.EventNotDraft(event.getName(), event.getStatus()));
    }

    private Either<Failure, Event> requireTicketTypes(Event event) {
        return event.getTicketTypes().isEmpty()
                ? Either.left(new Failure.EventHasNoTicketTypes(event.getName()))
                : Either.right(event);
    }

    private Either<Failure, Event> requireEventNotStarted(Event event) {
        return event.hasStarted(currentTime.get())
                ? Either.left(new Failure.EventAlreadyStarted(event.getName(), event.getStartsAt()))
                : Either.right(event);
    }

    private Either<Failure, Event> requireOnSale(Event event, Instant now) {
        if (event.getStatus() != EventStatus.PUBLISHED) {
            return Either.left(new Failure.EventNotOnSale(event.getName(), event.getStatus()));
        }
        if (event.hasStarted(now)) {
            return Either.left(new Failure.EventAlreadyStarted(event.getName(), event.getStartsAt()));
        }
        if (now.isBefore(event.getSalesOpenAt()) || now.isAfter(event.getSalesCloseAt())) {
            return Either.left(new Failure.OutsideSalesWindow(
                    event.getName(), event.getSalesOpenAt(), event.getSalesCloseAt()));
        }
        return Either.right(event);
    }

    private Either<Failure, Reservation> requireCustomer(Reservation reservation, CustomerId customerId) {
        return reservation.isOwnedBy(customerId)
                ? Either.right(reservation)
                : Either.left(new Failure.NotReservationOwner());
    }

    private Either<Failure, Reservation> requireActionable(Reservation reservation, Instant now) {
        if (reservation.getStatus() != Reservation.ReservationStatus.PENDING) {
            return Either.left(new Failure.ReservationNotPending(reservation.getStatus()));
        }
        if (reservation.isTimeExpired(now)) {
            expireIfNeeded(reservation, now);
            return Either.left(new Failure.ReservationExpired(reservation.getExpiresAt()));
        }
        return Either.right(reservation);
    }

    private Either<Failure, Tuple2<Event, List<ReservationLine>>> checkAllLines(
            Event event, CustomerId customerId, List<ReservationRequestLine> request, Instant now) {
        List<Reservation> active = reservations.findActiveByCustomerAndEvent(
                customerId, event.getId(), now);
        Either<Failure, Tuple2<Event, List<ReservationLine>>> answer =
                Either.right(Tuple.of(event, List.empty()));

        for (ReservationRequestLine line : request) {
            answer = answer.flatMap(state -> checkOneLine(state, line, active));
        }
        return answer;
    }

    private Either<Failure, Tuple2<Event, List<ReservationLine>>> checkOneLine(
            Tuple2<Event, List<ReservationLine>> state, ReservationRequestLine request,
            List<Reservation> active) {
        Event event = state._1;
        List<ReservationLine> lines = state._2;

        if (request.quantity() <= 0) {
            return Either.left(new Failure.ValidationError(
                    "quantity must be positive: " + request.quantity()));
        }

        return event.ticketType(request.ticketTypeId())
                .toEither((Failure) new Failure.TicketTypeNotOnEvent(
                        event.getName(), request.ticketTypeId().toString()))
                .flatMap(ticket -> requireAvailable(ticket, request.quantity()))
                .flatMap(ticket -> requireCustomerLimit(ticket, request.quantity(), active, lines))
                .map(ticket -> {
                    TicketType reservedTicket = ticket.reserve(request.quantity());
                    Event updated = event.withTicketTypeUpdated(reservedTicket);
                    ReservationLine reservationLine = ReservationLine.of(
                            ticket.getId(), ticket.getName(), ticket.getPrice(), request.quantity());
                    return Tuple.of(updated, lines.append(reservationLine));
                });
    }

    private Either<Failure, TicketType> requireAvailable(TicketType ticket, int requested) {
        return requested <= ticket.available()
                ? Either.right(ticket)
                : Either.left(new Failure.InsufficientSeats(
                        ticket.getName(), requested, ticket.available()));
    }

    private Either<Failure, TicketType> requireCustomerLimit(
            TicketType ticket, int requested, List<Reservation> active,
            List<ReservationLine> lines) {
        int alreadyHeld = active.flatMap(Reservation::getLines)
                .filter(line -> line.getTicketTypeId().equals(ticket.getId()))
                .map(ReservationLine::getQuantity)
                .sum().intValue();
        int inThisRequest = lines.filter(line -> line.getTicketTypeId().equals(ticket.getId()))
                .map(ReservationLine::getQuantity)
                .sum().intValue();
        int total = alreadyHeld + inThisRequest + requested;

        return total <= ticket.getPerCustomerLimit()
                ? Either.right(ticket)
                : Either.left(new Failure.PerCustomerLimitExceeded(
                        ticket.getName(), ticket.getPerCustomerLimit(), alreadyHeld + inThisRequest, requested));
    }

    private Either<Failure, Reservation> releaseSeats(Reservation reservation) {
        return findEvent(reservation.getEventId()).map(event -> {
            Event updated = event;
            for (ReservationLine line : reservation.getLines()) {
                TicketType ticket = updated.ticketType(line.getTicketTypeId())
                        .getOrElseThrow(() -> missingTicket(reservation, event));
                updated = updated.withTicketTypeUpdated(ticket.release(line.getQuantity()));
            }
            events.save(updated);
            return reservation;
        });
    }

    private Either<Failure, Reservation> sellReservedSeats(Reservation reservation) {
        return findEvent(reservation.getEventId()).map(event -> {
            Event updated = event;
            for (ReservationLine line : reservation.getLines()) {
                TicketType ticket = updated.ticketType(line.getTicketTypeId())
                        .getOrElseThrow(() -> missingTicket(reservation, event));
                updated = updated.withTicketTypeUpdated(ticket.confirm(line.getQuantity()));
            }
            events.save(updated);
            return reservation;
        });
    }

    private Reservation expireIfNeeded(Reservation reservation, Instant now) {
        if (reservation.isTimeExpired(now)) {
            releaseSeats(reservation);
            return reservations.save(reservation.expire());
        }
        return reservation;
    }

    private InvariantViolationException missingTicket(Reservation reservation, Event event) {
        return new InvariantViolationException(
                "reservation " + reservation.getId() + " references a missing ticket type on event " + event.getId());
    }

    private Either<Failure, Reservation> requireExactAmount(Reservation reservation, Money given) {
        Money expected = reservation.total();
        return expected.amount().compareTo(given.amount()) == 0
                ? Either.right(reservation)
                : Either.left(new Failure.PaymentAmountMismatch(expected, given));
    }
}
