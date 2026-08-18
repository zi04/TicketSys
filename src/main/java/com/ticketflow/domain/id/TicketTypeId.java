package com.ticketflow.domain.id;

import java.util.Objects;
import java.util.UUID;

public record TicketTypeId(UUID value) {

    public TicketTypeId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static TicketTypeId newId() {
        return new TicketTypeId(UUID.randomUUID());
    }

    public static TicketTypeId of(String raw) {
        return new TicketTypeId(UUID.fromString(raw));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
