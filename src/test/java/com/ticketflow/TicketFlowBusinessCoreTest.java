package com.ticketflow;

import com.ticketflow.domain.Event;
import com.ticketflow.domain.Money;
import com.ticketflow.domain.Order;
import com.ticketflow.domain.Reservation;
import com.ticketflow.domain.Reservation.ReservationStatus;
import com.ticketflow.domain.TicketType;
import com.ticketflow.domain.id.CustomerId;
import com.ticketflow.domain.id.OrganiserId;
import com.ticketflow.failure.Failure;
import com.ticketflow.storage.InMemoryEventRepository;
import com.ticketflow.storage.InMemoryOrderRepository;
import com.ticketflow.storage.InMemoryReservationRepository;
import io.vavr.collection.List;
import io.vavr.control.Either;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketFlowBusinessCoreTest {

    private static final Instant T0 = Instant.parse("2026-08-15T10:00:00Z");
    private static final OrganiserId ORGANISER = OrganiserId.of("organiser");
    private static final CustomerId ALICE = CustomerId.of("alice");

    @Test
    void reservationIsAtomicAndPaymentIsIdempotent() {
        Fixture fixture = new Fixture();
        Event event = fixture.publishedEvent(5, 5);
        TicketType general = event.getTicketTypes().head();

        Either<Failure, Reservation> failedReservation = fixture.flow.reserve(
                event.getId(), ALICE, List.of(
                        new ReservationRequestLine(general.getId(), 2),
                        new ReservationRequestLine(general.getId(), 4)));
        assertTrue(failedReservation.isLeft());
        assertInstanceOf(Failure.InsufficientSeats.class, failedReservation.getLeft());
        assertEquals(5, fixture.events.findById(event.getId()).get().getTicketTypes().head().available());

        Reservation reservation = fixture.flow.reserve(
                        event.getId(), ALICE, List.of(new ReservationRequestLine(general.getId(), 2)))
                .get();
        Order first = fixture.flow.pay(reservation.getId(), ALICE, Money.of("20.00"), "payment-1").get();
        Order replay = fixture.flow.pay(reservation.getId(), ALICE, Money.of("20.00"), "payment-1").get();

        assertEquals(first.getId(), replay.getId());
        TicketType updated = fixture.events.findById(event.getId()).get().getTicketTypes().head();
        assertEquals(0, updated.getReserved());
        assertEquals(2, updated.getSold());
        assertEquals(3, updated.available());
    }

    @Test
    void expiredReservationReleasesSeatsAndCannotBePaid() {
        Fixture fixture = new Fixture();
        Event event = fixture.publishedEvent(5, 5);
        TicketType general = event.getTicketTypes().head();
        Reservation reservation = fixture.flow.reserve(
                        event.getId(), ALICE, List.of(new ReservationRequestLine(general.getId(), 2)))
                .get();

        fixture.clock.advance(Reservation.HOLD);
        Either<Failure, Order> payment = fixture.flow.pay(
                reservation.getId(), ALICE, Money.of("20.00"), "expired-payment");

        assertTrue(payment.isLeft());
        assertInstanceOf(Failure.ReservationExpired.class, payment.getLeft());
        assertEquals(ReservationStatus.EXPIRED, fixture.flow.get(reservation.getId()).get().getStatus());
        assertEquals(5, fixture.events.findById(event.getId()).get().getTicketTypes().head().available());
    }

    private static final class Fixture {
        private final InMemoryEventRepository events = new InMemoryEventRepository();
        private final InMemoryReservationRepository reservationStore = new InMemoryReservationRepository();
        private final TestTimeProvider clock = new TestTimeProvider(T0);
        private final TicketFlowManager flow = new TicketFlowManager(
                events, reservationStore, new InMemoryOrderRepository(), clock);

        private Event publishedEvent(int allocation, int limit) {
            Event event = flow.createEvent(
                            ORGANISER, "Test event", T0.minusSeconds(1), T0.plusSeconds(3_600), T0.plusSeconds(7_200))
                    .get();
            flow.addTicketType(event.getId(), ORGANISER, "General", Money.of("10.00"), allocation, limit)
                    .get();
            return flow.publish(event.getId(), ORGANISER).get();
        }
    }

    private static final class TestTimeProvider implements Supplier<Instant> {
        private Instant current;

        private TestTimeProvider(Instant start) {
            this.current = start;
        }

        @Override
        public Instant get() {
            return current;
        }

        private void advance(Duration amount) {
            current = current.plus(amount);
        }
    }
}
