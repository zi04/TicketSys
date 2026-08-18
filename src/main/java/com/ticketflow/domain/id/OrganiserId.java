package com.ticketflow.domain.id;

import java.util.Objects;

public record OrganiserId(String value) {

    public OrganiserId {
        Objects.requireNonNull(value, "value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("organiser id must not be blank");
        }
    }

    public static OrganiserId of(String raw) {
        return new OrganiserId(raw);
    }

    @Override
    public String toString() {
        return value;
    }
}
