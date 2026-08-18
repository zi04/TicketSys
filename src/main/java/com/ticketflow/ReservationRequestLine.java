package com.ticketflow;

import com.ticketflow.domain.id.TicketTypeId;

/** One ticket type and the number requested. */
public record ReservationRequestLine(TicketTypeId ticketTypeId, int quantity) {
}
