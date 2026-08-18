package com.ticketflow.domain;

import com.ticketflow.domain.id.EventId;
import com.ticketflow.domain.id.TicketTypeId;
import lombok.Getter;
import java.util.Objects;

@Getter
public final class TicketType {

    private final TicketTypeId id;
    private final EventId eventId;
    private final String name;
    private final Money price;
    private final int allocation;
    private final int perCustomerLimit;
    private final int reserved;
    private final int sold;

    private TicketType(TicketTypeId id, EventId eventId, String name, Money price,
                        int allocation, int perCustomerLimit, int reserved, int sold) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(price, "price must not be null");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (allocation <= 0) {
            throw new IllegalArgumentException("allocation must be positive: " + allocation);
        }
        if (perCustomerLimit <= 0) {
            throw new IllegalArgumentException("perCustomerLimit must be positive: " + perCustomerLimit);
        }
        if (perCustomerLimit > allocation) {
            throw new IllegalArgumentException("perCustomerLimit cannot exceed allocation");
        }
        if (reserved < 0 || sold < 0) {
            throw new InvariantViolationException("reserved and sold must be >= 0 (reserved=" + reserved + ", sold=" + sold + ")");
        }
        if (reserved + sold > allocation) {
            throw new InvariantViolationException(
                    "reserved + sold must not exceed allocation (reserved=" + reserved + ", sold=" + sold + ", allocation=" + allocation + ")");
        }
        this.id = id;
        this.eventId = eventId;
        this.name = name;
        this.price = price;
        this.allocation = allocation;
        this.perCustomerLimit = perCustomerLimit;
        this.reserved = reserved;
        this.sold = sold;
    }

    public static TicketType create(EventId eventId, String name, Money price, int allocation, int perCustomerLimit) {
        return new TicketType(TicketTypeId.newId(), eventId, name, price, allocation, perCustomerLimit, 0, 0);
    }

    public int available() {
        return allocation - reserved - sold;
    }
    public TicketType reserve(int quantity) {
        requirePositive(quantity);
        return copy(this.reserved + quantity, this.sold);
    }

    public TicketType release(int quantity) {
        requirePositive(quantity);
        return copy(this.reserved - quantity, this.sold);
    }

    public TicketType confirm(int quantity) {
        requirePositive(quantity);
        return copy(this.reserved - quantity, this.sold + quantity);
    }

    private TicketType copy(int newReserved, int newSold) {
        return new TicketType(id, eventId, name, price, allocation, perCustomerLimit,
                newReserved, newSold);
    }

    private static void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive: " + quantity);
        }
    }
}
