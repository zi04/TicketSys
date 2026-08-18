package com.ticketflow.domain;

import com.ticketflow.domain.id.*;
import io.vavr.collection.List;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Getter
public final class Reservation {

    public static final Duration HOLD = Duration.ofMinutes(15);
    public enum ReservationStatus {
        PENDING, CONFIRMED, CANCELLED, EXPIRED
    }

    private final ReservationId id;
    private final EventId eventId;
    private final CustomerId customerId;
    private final List<ReservationLine> lines;
    private final ReservationStatus status;
    private final Instant createdAt;
    private final Instant expiresAt;

    private Reservation(ReservationId id, EventId eventId, CustomerId customerId, List<ReservationLine> lines,
                         ReservationStatus status, Instant createdAt, Instant expiresAt) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("a reservation must cover at least one ticket type");
        }
        this.id = id;
        this.eventId = eventId;
        this.customerId = customerId;
        this.lines = lines;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public static Reservation hold(EventId eventId, CustomerId customerId, List<ReservationLine> lines, Instant now) {
        return new Reservation(ReservationId.newId(), eventId, customerId, lines,
                ReservationStatus.PENDING, now, now.plus(HOLD));
    }

    public Money total() {
        return lines.foldLeft(Money.zero(), (acc, line) -> acc.add(line.lineTotal()));
    }

    public boolean isOwnedBy(CustomerId candidate) {
        return this.customerId.equals(candidate);
    }

    public boolean isTimeExpired(Instant now) {
        return status == ReservationStatus.PENDING && !now.isBefore(expiresAt);
    }

    public boolean isActionable(Instant now) {
        return status == ReservationStatus.PENDING && !isTimeExpired(now);
    }

    public Reservation confirm() {
        requireStatus(ReservationStatus.PENDING, "confirm");
        return copy(ReservationStatus.CONFIRMED);
    }

    public Reservation cancel() {
        requireStatus(ReservationStatus.PENDING, "cancel");
        return copy(ReservationStatus.CANCELLED);
    }

    public Reservation expire() {
        requireStatus(ReservationStatus.PENDING, "expire");
        return copy(ReservationStatus.EXPIRED);
    }

    private Reservation copy(ReservationStatus newStatus) {
        return new Reservation(id, eventId, customerId, lines, newStatus, createdAt, expiresAt);
    }

    private void requireStatus(ReservationStatus required, String action) {
        if (this.status != required) {
            throw new InvariantViolationException(
                    "cannot " + action + " a reservation in status " + this.status + "  the service layer should have checked isActionable() first");
        }
    }
}
