package com.ticketflow.domain.id;

import java.util.Objects;

public record CustomerId(String value) {

    public CustomerId {
        Objects.requireNonNull(value, "value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("customer id must not be blank");
        }
    }

    public static CustomerId of(String raw) {
        return new CustomerId(raw);
    }

    @Override
    public String toString() {
        return value;
    }
}
