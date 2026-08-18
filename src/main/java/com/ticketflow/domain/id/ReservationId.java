package com.ticketflow.domain.id;

import java.util.Objects;
import java.util.UUID;

public record ReservationId(UUID value) {

    public ReservationId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static ReservationId newId() {
        return new ReservationId(UUID.randomUUID());
    }

    public static ReservationId of(String raw) {
        return new ReservationId(UUID.fromString(raw));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
