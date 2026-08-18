package com.ticketflow.domain;

import com.ticketflow.domain.id.TicketTypeId;
import lombok.Getter;

import java.util.Objects;

@Getter
public final class ReservationLine {

    private final TicketTypeId ticketTypeId;
    private final String ticketTypeName;
    private final Money unitPrice;
    private final int quantity;

    private ReservationLine(TicketTypeId ticketTypeId, String ticketTypeName, Money unitPrice, int quantity) {
        Objects.requireNonNull(ticketTypeId, "ticketTypeId must not be null");
        Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        if (ticketTypeName == null || ticketTypeName.isBlank()) {
            throw new IllegalArgumentException("ticketTypeName must not be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive: " + quantity);
        }
        this.ticketTypeId = ticketTypeId;
        this.ticketTypeName = ticketTypeName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public static ReservationLine of(TicketTypeId ticketTypeId, String ticketTypeName,
                                      Money unitPrice, int quantity) {
        return new ReservationLine(ticketTypeId, ticketTypeName, unitPrice, quantity);
    }

    public Money lineTotal() {
        return unitPrice.multiply(quantity);
    }
}
