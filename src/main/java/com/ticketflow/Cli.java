package com.ticketflow;

import com.ticketflow.domain.Event;
import com.ticketflow.domain.Money;
import com.ticketflow.domain.Reservation;
import com.ticketflow.domain.ReservationLine;
import com.ticketflow.domain.TicketType;
import com.ticketflow.domain.id.CustomerId;
import com.ticketflow.domain.id.EventId;
import com.ticketflow.domain.id.OrganiserId;
import com.ticketflow.domain.id.OrderId;
import com.ticketflow.domain.id.ReservationId;
import com.ticketflow.domain.id.TicketTypeId;
import com.ticketflow.failure.Failure;
import io.vavr.collection.List;
import io.vavr.control.Either;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Scanner;

public final class Cli {

    private final TicketFlowManager ticketFlow;

    public Cli(TicketFlowManager ticketFlow) {
        this.ticketFlow = ticketFlow;
    }

    public void run() {
        System.out.println("TicketFlow");
        System.out.println("Type help to see the commands.");
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            if (!scanner.hasNextLine()) {
                break;
            }
            String line = scanner.nextLine();
            try {
                if (!dispatch(line)) {
                    break;
                }
            } catch (Exception e) {
                System.out.println("Something went wrong: " + e.getMessage());
            }
        }
        System.out.println("Bye.");
    }

    private boolean dispatch(String line) {
        String trimmed = line == null ? "" : line.trim();
        if (trimmed.isEmpty()) {
            return true;
        }
        String[] tokens = trimmed.split("\\s+");
        String command = tokens[0];

        switch (command) {
            case "help" -> printHelp();
            case "exit", "quit" -> {
                return false;
            }
            case "create-event" -> handle(tokens, this::createEvent);
            case "add-ticket" -> handle(tokens, this::addTicket);
            case "publish" -> handle(tokens, this::publish);
            case "list-events" -> listEvents();
            case "show-event" -> handle(tokens, this::showEvent);
            case "reserve" -> handle(tokens, this::reserve);
            case "show-reservation" -> handle(tokens, this::showReservation);
            case "cancel" -> handle(tokens, this::cancel);
            case "expire" -> expire();
            case "pay" -> handle(tokens, this::pay);
            case "show-order" -> handle(tokens, this::showOrder);
            case "stats" -> stats();
            default -> System.out.println("Unknown command '" + command + "'. Type 'help' for the list.");
        }
        return true;
    }

    @FunctionalInterface
    private interface CommandHandler {
        void handle(String[] args);
    }

    private void handle(String[] tokens, CommandHandler handler) {
        try {
            handler.handle(tokens);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Missing argument. Type 'help' to see what this command needs.");
        } catch (NumberFormatException e) {
            System.out.println("Expected a number where you gave '" + e.getMessage() + "'.");
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid input: " + e.getMessage());
        }
    }


    private void createEvent(String[] a) {
        OrganiserId organiserId = OrganiserId.of(a[1]);
        String name = a[2];
        LocalDate today = LocalDate.now(ZoneId.systemDefault());

        print(ticketFlow.createEvent(organiserId, name,
                eventTime(a[3], today),
                eventTime(a[4], today),
                eventTime(a[5], today)),
                e -> "Event " + e.getId() + " '" + e.getName() + "' created as " + e.getStatus());
    }

    private void addTicket(String[] a) {
        EventId eventId = EventId.of(a[1]);
        OrganiserId organiserId = OrganiserId.of(a[2]);
        String name = a[3];
        Money price = Money.of(a[4]);
        int allocation = Integer.parseInt(a[5]);
        int perCustomerLimit = Integer.parseInt(a[6]);

        print(ticketFlow.addTicketType(eventId, organiserId, name, price, allocation, perCustomerLimit),
                e -> "Ticket type '" + name + "' added to event " + e.getId()
                        + " (" + allocation + " seats @ " + price.display() + ", limit " + perCustomerLimit + "/customer)");
    }

    private void publish(String[] a) {
        EventId eventId = EventId.of(a[1]);
        OrganiserId organiserId = OrganiserId.of(a[2]);
        print(ticketFlow.publish(eventId, organiserId), e -> "Event " + e.getId() + " published.");
    }

    private void listEvents() {
        List<Event> all = ticketFlow.listAll();
        if (all.isEmpty()) {
            System.out.println("No events yet.");
            return;
        }
        for (Event event : all) {
            System.out.println(event.getId() + " - " + event.getName()
                    + " - " + event.getStatus() + " - starts " + event.getStartsAt());
        }
    }

    private void showEvent(String[] a) {
        EventId eventId = EventId.of(a[1]);
        print(ticketFlow.get(eventId), e -> {
            StringBuilder sb = new StringBuilder();
            sb.append("Event: ").append(e.getName()).append('\n');
            sb.append("Id: ").append(e.getId()).append('\n');
            sb.append("Status: ").append(e.getStatus()).append('\n');
            sb.append("Sales: ").append(e.getSalesOpenAt()).append(" to ").append(e.getSalesCloseAt())
                    .append(" (starts ").append(e.getStartsAt()).append(")\n");
            for (TicketType tt : e.getTicketTypes()) {
                sb.append("Ticket: ").append(tt.getName()).append(" (id ").append(tt.getId()).append(")")
                        .append(" price=").append(tt.getPrice().display())
                        .append(" available=").append(tt.available())
                        .append(" / ").append(tt.getAllocation())
                        .append(" reserved=").append(tt.getReserved())
                        .append(" sold=").append(tt.getSold())
                        .append('\n');
            }
            return sb.toString().stripTrailing();
        });
    }

    private void reserve(String[] a) {
        EventId eventId = EventId.of(a[1]);
        CustomerId customerId = CustomerId.of(a[2]);
        List<ReservationRequestLine> lines = List.of(a[3].split(","))
                .map(part -> {
                    String[] kv = part.split(":");
                    return new ReservationRequestLine(TicketTypeId.of(kv[0]), Integer.parseInt(kv[1]));
                });

        print(ticketFlow.reserve(eventId, customerId, lines), r ->
                "Reservation " + r.getId() + " held for " + customerId + ": " + describeLines(r.getLines())
                        + " = " + r.total().display() + ", expires at " + r.getExpiresAt());
    }

    private void showReservation(String[] a) {
        ReservationId reservationId = ReservationId.of(a[1]);
        print(ticketFlow.get(reservationId), r ->
                "Reservation " + r.getId() + " — " + r.getStatus() + " — customer " + r.getCustomerId()
                        + " — " + describeLines(r.getLines()) + " = " + r.total().display()
                        + " — created " + r.getCreatedAt() + " — expires " + r.getExpiresAt());
    }

    private void cancel(String[] a) {
        ReservationId reservationId = ReservationId.of(a[1]);
        CustomerId customerId = CustomerId.of(a[2]);
        print(ticketFlow.cancel(reservationId, customerId), r -> "Reservation " + r.getId() + " cancelled, seats released.");
    }

    private void expire() {
        List<Reservation> expired = ticketFlow.expireDue();
        if (expired.isEmpty()) {
            System.out.println("Nothing to expire.");
        } else {
            expired.forEach(r -> System.out.println("Reservation " + r.getId() + " expired, seats released."));
        }
    }

    private void pay(String[] a) {
        ReservationId reservationId = ReservationId.of(a[1]);
        CustomerId customerId = CustomerId.of(a[2]);
        Money amount = Money.of(a[3]);
        String paymentReference = a[4];

        print(ticketFlow.pay(reservationId, customerId, amount, paymentReference), order ->
                "Payment accepted. Order " + order.getId() + " created for " + order.getTotal().display()
                        + " (ref " + order.getPaymentReference() + ")");
    }

    private void showOrder(String[] a) {
        OrderId orderId = OrderId.of(a[1]);
        print(ticketFlow.getOrder(orderId), o ->
                "Order " + o.getId() + " — reservation " + o.getReservationId() + " — customer " + o.getCustomerId()
                        + " — " + describeLines(o.getLines()) + " = " + o.getTotal().display() + " — paid " + o.getPaidAt());
    }

    private void stats() {
        List<TicketType> allTicketTypes = ticketFlow.listAll().flatMap(Event::getTicketTypes);

        if (allTicketTypes.isEmpty()) {
            System.out.println("No ticket types yet.");
            return;
        }

        int totalAllocation = allTicketTypes.map(TicketType::getAllocation).sum().intValue();
        int totalReserved = allTicketTypes.map(TicketType::getReserved).sum().intValue();
        int totalSold = allTicketTypes.map(TicketType::getSold).sum().intValue();
        int totalAvailable = allTicketTypes.map(TicketType::available).sum().intValue();
        var mostReserved = allTicketTypes.maxBy(TicketType::getReserved);

        System.out.println("Across " + allTicketTypes.size() + " ticket type(s): "
                + "allocation=" + totalAllocation + " reserved=" + totalReserved
                + " sold=" + totalSold + " available=" + totalAvailable);
        mostReserved.forEach(tt -> System.out.println("Most held right now: " + tt.getName() + " (" + tt.getReserved() + " reserved)"));
    }

    private void printHelp() {
        System.out.println("""
                Commands:
                  create-event <organiserId> <name> <opensAt> <closesAt> <startsAt>
                  add-ticket <eventId> <organiserId> <name> <price> <allocation> <perCustomerLimit>
                  publish <eventId> <organiserId>
                  list-events
                  show-event <eventId>
                  reserve <eventId> <customerId> <ticketTypeId:qty>
                  show-reservation <reservationId>
                  cancel <reservationId> <customerId>
                  expire
                  pay <reservationId> <customerId> <amount> <paymentReference>
                  show-order <orderId>
                  stats
                  help
                  exit
                Use times like 10AM, 8PM, and 9PM. They are for today.
                Money is entered in USD. Names and ids cannot contain spaces.""");
    }


    private <T> void print(Either<Failure, T> result, java.util.function.Function<T, String> onSuccess) {
        result.peek(value -> System.out.println(onSuccess.apply(value)))
                .peekLeft(failure -> System.out.println("Failed: " + failure.message()));
    }

    private static String describeLines(List<ReservationLine> lines) {
        return lines.map(l -> l.getQuantity() + "x " + l.getTicketTypeName()).mkString(", ");
    }

    private static Instant eventTime(String value, LocalDate date) {
        try {
            DateTimeFormatter format = DateTimeFormatter.ofPattern("ha", Locale.ENGLISH);
            LocalTime time = LocalTime.parse(value.toUpperCase(Locale.ENGLISH), format);
            return date.atTime(time).atZone(ZoneId.systemDefault()).toInstant();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("time must look like 10AM or 8PM");
        }
    }

}
