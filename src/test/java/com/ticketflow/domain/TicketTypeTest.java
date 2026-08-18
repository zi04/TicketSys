package com.ticketflow.domain;

import com.ticketflow.domain.id.EventId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TicketTypeTest {

    private final EventId eventId = EventId.newId();

    private TicketType vip(int allocation, int perCustomerLimit) {
        return TicketType.create(eventId, "VIP", Money.of("75.00"), allocation, perCustomerLimit);
    }

    @Test
    void availableStartsAtFullAllocation() {
        TicketType tt = vip(100, 4);
        assertEquals(100, tt.available());
        assertEquals(0, tt.getReserved());
        assertEquals(0, tt.getSold());
    }

    @Test
    void reserveMovesSeatsOutOfAvailable() {
        TicketType tt = vip(100, 4).reserve(30);
        assertEquals(30, tt.getReserved());
        assertEquals(0, tt.getSold());
        assertEquals(70, tt.available());
    }

    @Test
    void releaseReturnsSeatsToAvailable() {
        TicketType tt = vip(100, 4).reserve(30).release(20);
        assertEquals(10, tt.getReserved());
        assertEquals(90, tt.available());
    }

    @Test
    void confirmMovesFromReservedToSold() {
        TicketType tt = vip(100, 4).reserve(30).confirm(20);
        assertEquals(10, tt.getReserved());
        assertEquals(20, tt.getSold());
        assertEquals(70, tt.available());
    }

    @Test
    void availableAlwaysEqualsAllocationMinusReservedMinusSold() {
        TicketType tt = vip(100, 4).reserve(30).confirm(10).reserve(15).release(5);
        assertEquals(tt.getAllocation() - tt.getReserved() - tt.getSold(), tt.available());
    }

    @Test
    void reservingMoreThanAllocationViolatesR7() {
        TicketType tt = vip(10, 4);
        assertThrows(InvariantViolationException.class, () -> tt.reserve(11));
    }

    @Test
    void reservingBeyondWhatIsAlreadyHeldStillCannotExceedAllocation() {
        TicketType tt = vip(10, 10).reserve(7);
        assertThrows(InvariantViolationException.class, () -> tt.reserve(4));
    }

    @Test
    void releasingMoreThanIsReservedGoesNegativeAndViolatesR7() {
        TicketType tt = vip(10, 10).reserve(3);
        assertThrows(InvariantViolationException.class, () -> tt.release(4));
    }

    @Test
    void confirmingMoreThanIsReservedGoesNegativeAndViolatesR7() {
        TicketType tt = vip(10, 10).reserve(3);
        assertThrows(InvariantViolationException.class, () -> tt.confirm(4));
    }

    @Test
    void zeroOrNegativeQuantityIsRejected() {
        TicketType tt = vip(10, 10);
        assertThrows(IllegalArgumentException.class, () -> tt.reserve(0));
        assertThrows(IllegalArgumentException.class, () -> tt.reserve(-1));
    }

    @Test
    void perCustomerLimitCannotExceedAllocation() {
        assertThrows(IllegalArgumentException.class, () -> vip(5, 6));
    }
}
