package com.ticketflow.domain.id;

import java.util.Objects;
import java.util.UUID;

public record EventId(UUID value) {

    public EventId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static EventId newId() {
        return new EventId(UUID.randomUUID());
    }
    public static EventId of(String raw) {
        return new EventId(UUID.fromString(raw));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
