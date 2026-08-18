package com.ticketflow.domain;

import com.ticketflow.domain.id.CustomerId;
import com.ticketflow.domain.id.EventId;
import com.ticketflow.domain.id.TicketTypeId;
import com.ticketflow.domain.Reservation.ReservationStatus;
import io.vavr.collection.List;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservationTest {

    private final EventId eventId = EventId.newId();
    private final CustomerId customerId = CustomerId.of("cust-1");
    private final Instant t0 = Instant.parse("2026-08-15T10:00:00Z");

    private ReservationLine line(int quantity) {
        return ReservationLine.of(
                TicketTypeId.newId(), "VIP", Money.of("75.00"), quantity);
    }

    private Reservation held(Instant now) {
        return Reservation.hold(eventId, customerId, List.of(line(2)), now);
    }

    @Test
    void holdStartsPendingWithAFifteenMinuteExpiry() {
        Reservation r = held(t0);
        assertEquals(ReservationStatus.PENDING, r.getStatus());
        assertEquals(t0, r.getCreatedAt());
        assertEquals(t0.plus(Reservation.HOLD), r.getExpiresAt());
    }

    @Test
    void aReservationMustCoverAtLeastOneLine() {
        assertThrows(IllegalArgumentException.class,
                () -> Reservation.hold(eventId, customerId, List.empty(), t0));
    }

    @Test
    void totalSumsAcrossLines() {
        Reservation r = Reservation.hold(eventId, customerId, List.of(line(2), line(1)), t0);
        assertEquals(Money.of("225.00"), r.total());
    }

    @Test
    void isNotTimeExpiredBeforeFifteenMinutesPass() {
        Reservation r = held(t0);
        assertFalse(r.isTimeExpired(t0.plusSeconds(14 * 60)));
    }

    @Test
    void isTimeExpiredExactlyAtFifteenMinutes() {
        Reservation r = held(t0);
        assertTrue(r.isTimeExpired(t0.plus(Reservation.HOLD)));
    }

    @Test
    void isTimeExpiredWellAfterFifteenMinutes() {
        Reservation r = held(t0);
        assertTrue(r.isTimeExpired(t0.plusSeconds(60 * 60)));
    }

    @Test
    void isActionableWhilePendingAndUnexpired() {
        Reservation r = held(t0);
        assertTrue(r.isActionable(t0.plusSeconds(60)));
    }

    @Test
    void isNotActionableOnceTimeExpired() {
        Reservation r = held(t0);
        assertFalse(r.isActionable(t0.plus(Reservation.HOLD)));
    }

    @Test
    void isNotActionableOnceConfirmed() {
        Reservation r = held(t0).confirm();
        assertFalse(r.isActionable(t0.plusSeconds(60)));
    }

    @Test
    void confirmMovesPendingToConfirmed() {
        Reservation r = held(t0).confirm();
        assertEquals(ReservationStatus.CONFIRMED, r.getStatus());
    }

    @Test
    void cancelMovesPendingToCancelled() {
        Reservation r = held(t0).cancel();
        assertEquals(ReservationStatus.CANCELLED, r.getStatus());
    }

    @Test
    void expireMovesPendingToExpired() {
        Reservation r = held(t0).expire();
        assertEquals(ReservationStatus.EXPIRED, r.getStatus());
    }

    @Test
    void cannotConfirmATerminalReservation() {
        Reservation cancelled = held(t0).cancel();
        assertThrows(InvariantViolationException.class, cancelled::confirm);
    }

    @Test
    void cannotCancelATerminalReservation() {
        Reservation confirmed = held(t0).confirm();
        assertThrows(InvariantViolationException.class, confirmed::cancel);
    }

    @Test
    void cannotExpireATerminalReservation() {
        Reservation confirmed = held(t0).confirm();
        assertThrows(InvariantViolationException.class, confirmed::expire);
    }

    @Test
    void cannotConfirmTwice() {
        Reservation confirmed = held(t0).confirm();
        assertThrows(InvariantViolationException.class, confirmed::confirm);
    }

    @Test
    void isOwnedByChecksCustomerId() {
        Reservation r = held(t0);
        assertTrue(r.isOwnedBy(customerId));
        assertFalse(r.isOwnedBy(CustomerId.of("someone-else")));
    }
}
