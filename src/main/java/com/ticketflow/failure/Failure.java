package com.ticketflow.failure;

import com.ticketflow.domain.Event.EventStatus;
import com.ticketflow.domain.Money;
import com.ticketflow.domain.Reservation.ReservationStatus;
import java.time.Instant;

public sealed interface Failure {

    String message();

    record NotFound(String entityType, String id) implements Failure {
        public String message() {
            return "No " + entityType + " found with id " + id;
        }
    }

    record NotEventOwner(String eventName) implements Failure {
        public String message() {
            return "Event '" + eventName + "' is not yours — only the organiser who created it may publish it";
        }
    }

    record NotReservationOwner() implements Failure {
        public String message() {
            return "This reservation is not yours — only the customer who made it may pay or cancel it";
        }
    }

    record EventNotDraft(String eventName, EventStatus currentStatus) implements Failure {
        public String message() {
            return "Event '" + eventName + "' is " + currentStatus + " — ticket types can only be added while it is a draft";
        }
    }

    record EventHasNoTicketTypes(String eventName) implements Failure {
        public String message() {
            return "Event '" + eventName + "' cannot be published without at least one ticket type";
        }
    }

    record EventAlreadyStarted(String eventName, Instant startsAt) implements Failure {
        public String message() {
            return "Event '" + eventName + "' already started at " + startsAt + " — it can no longer be published or sold";
        }
    }

    record EventNotOnSale(String eventName, EventStatus currentStatus) implements Failure {
        public String message() {
            return "Event '" + eventName + "' is not on sale, its current status is " + currentStatus;
        }
    }

    record OutsideSalesWindow(String eventName, Instant opensAt, Instant closesAt) implements Failure {
        public String message() {
            return "Event '" + eventName + "' is only on sale between " + opensAt + " and " + closesAt;
        }
    }

    record EmptyReservationRequest() implements Failure {
        public String message() {
            return "A reservation needs at least one ticket type and quantity";
        }
    }

    record TicketTypeNotOnEvent(String eventName, String ticketTypeId) implements Failure {
        public String message() {
            return "Ticket type " + ticketTypeId + " does not exist on event '" + eventName + "'";
        }
    }

    record InsufficientSeats(String ticketTypeName, int requested, int available) implements Failure {
        public String message() {
            return "Only " + available + " seats left for " + ticketTypeName + ", you asked for " + requested;
        }
    }

    record PerCustomerLimitExceeded(String ticketTypeName, int limit, int alreadyHeld, int requested) implements Failure {
        public String message() {
            return "You may take at most " + limit + " of " + ticketTypeName
                    + " (you already hold " + alreadyHeld + " and asked for " + requested + " more)";
        }
    }

    record ReservationNotPending(ReservationStatus currentStatus) implements Failure {
        public String message() {
            return "This reservation is " + currentStatus + ", not pending — it cannot be paid or cancelled";
        }
    }

    record ReservationExpired(Instant expiredAt) implements Failure {
        public String message() {
            return "This reservation expired at " + expiredAt + " — the seats have gone back into the pool";
        }
    }

    record PaymentAmountMismatch(Money expected, Money given) implements Failure {
        public String message() {
            return "Expected a payment of " + expected.display() + ", got " + given.display();
        }
    }

    record ValidationError(String detail) implements Failure {
        public String message() {
            return detail;
        }
    }
}
