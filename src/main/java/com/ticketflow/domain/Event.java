package com.ticketflow.domain;

import com.ticketflow.domain.id.EventId;
import com.ticketflow.domain.id.OrganiserId;
import com.ticketflow.domain.id.TicketTypeId;
import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;

@Getter
public final class Event {

    public enum EventStatus {
        DRAFT, PUBLISHED
    }

    private final EventId id;
    private final OrganiserId organiserId;
    private final String name;
    private final Instant salesOpenAt;
    private final Instant salesCloseAt;
    private final Instant startsAt;
    private final EventStatus status;
    private final List<TicketType> ticketTypes;

    private Event(EventId id, OrganiserId organiserId, String name, Instant salesOpenAt,
                  Instant salesCloseAt, Instant startsAt, EventStatus status, List<TicketType> ticketTypes) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(organiserId, "organiserId must not be null");
        Objects.requireNonNull(salesOpenAt, "salesOpenAt must not be null");
        Objects.requireNonNull(salesCloseAt, "salesCloseAt must not be null");
        Objects.requireNonNull(startsAt, "startsAt must not be null");
        Objects.requireNonNull(status, "status must not be null");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (salesOpenAt.isAfter(salesCloseAt)) {
            throw new IllegalArgumentException("salesOpenAt must not be after salesCloseAt");
        }
        if (salesCloseAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("salesCloseAt must not be after startsAt");
        }
        this.id = id;
        this.organiserId = organiserId;
        this.name = name;
        this.salesOpenAt = salesOpenAt;
        this.salesCloseAt = salesCloseAt;
        this.startsAt = startsAt;
        this.status = status;
        this.ticketTypes = ticketTypes == null ? List.empty() : ticketTypes;
    }

    public static Event draft(OrganiserId organiserId, String name, Instant salesOpenAt, Instant salesCloseAt, Instant startsAt) {
        return new Event(EventId.newId(), organiserId, name, salesOpenAt, salesCloseAt, startsAt,
                EventStatus.DRAFT, List.empty());
    }

    public boolean isOwnedBy(OrganiserId candidate) {
        return this.organiserId.equals(candidate);
    }
    public boolean hasStarted(Instant now) {
        return !now.isBefore(startsAt);
    }

    public boolean isOnSale(Instant now) {
        return status == EventStatus.PUBLISHED
                && !now.isBefore(salesOpenAt)
                && !now.isAfter(salesCloseAt)
                && !hasStarted(now);
    }

    public Option<TicketType> ticketType(TicketTypeId ticketTypeId) {
        return ticketTypes.find(tt -> tt.getId().equals(ticketTypeId));
    }

    public Event withTicketTypeAdded(TicketType ticketType) {
        return copy(status, this.ticketTypes.append(ticketType));
    }

    public Event withTicketTypeUpdated(TicketType updated) {
        List<TicketType> replaced = ticketTypes.map(tt -> tt.getId().equals(updated.getId()) ? updated : tt);
        return copy(status, replaced);
    }

    public Event withStatus(EventStatus newStatus) {
        return copy(newStatus, ticketTypes);
    }

    private Event copy(EventStatus newStatus, List<TicketType> newTicketTypes) {
        return new Event(id, organiserId, name, salesOpenAt, salesCloseAt, startsAt,
                newStatus, newTicketTypes);
    }
}
