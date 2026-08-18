package com.ticketflow.storage;

import com.ticketflow.domain.Reservation;
import com.ticketflow.domain.Reservation.ReservationStatus;
import com.ticketflow.domain.id.*;
import io.vavr.collection.List;
import io.vavr.control.Option;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public final class InMemoryReservationRepository implements ReservationRepository {
    private final Map<ReservationId, Reservation> reservations = new LinkedHashMap<>();

    @Override
    public Option<Reservation> findById(ReservationId id) {
        return Option.of(reservations.get(id));
    }

    @Override
    public Reservation save(Reservation entity) {
        reservations.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public List<Reservation> findAll() {
        return List.ofAll(reservations.values());
    }

    @Override
    public List<Reservation> findActiveByCustomerAndEvent(CustomerId customerId, EventId eventId, Instant now) {
        return findAll()
                .filter(r -> r.getCustomerId().equals(customerId) && r.getEventId().equals(eventId))
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED
                        || (r.getStatus() == ReservationStatus.PENDING && !r.isTimeExpired(now)));
    }

    @Override
    public List<Reservation> findExpirable(Instant now) {
        return findAll().filter(r -> r.isTimeExpired(now));
    }
}
