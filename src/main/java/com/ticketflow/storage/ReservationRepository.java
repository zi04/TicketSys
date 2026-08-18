package com.ticketflow.storage;

import com.ticketflow.domain.Reservation;
import com.ticketflow.domain.id.CustomerId;
import com.ticketflow.domain.id.EventId;
import com.ticketflow.domain.id.ReservationId;
import io.vavr.collection.List;
import java.time.Instant;

public interface ReservationRepository extends Repository<Reservation, ReservationId> {

    List<Reservation> findActiveByCustomerAndEvent(CustomerId customerId, EventId eventId, Instant now);
    List<Reservation> findExpirable(Instant now);
}
