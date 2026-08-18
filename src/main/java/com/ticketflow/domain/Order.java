package com.ticketflow.domain;

import com.ticketflow.domain.id.*;
import io.vavr.collection.List;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;

@Getter
public final class Order {

    private final OrderId id;
    private final ReservationId reservationId;
    private final EventId eventId;
    private final CustomerId customerId;
    private final List<ReservationLine> lines;
    private final Money total;
    private final String paymentReference;
    private final Instant paidAt;

    private Order(OrderId id, ReservationId reservationId, EventId eventId, CustomerId customerId,
                  List<ReservationLine> lines, Money total, String paymentReference, Instant paidAt) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(reservationId, "reservationId must not be null");
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(total, "total must not be null");
        Objects.requireNonNull(paidAt, "paidAt must not be null");
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("an order must cover at least one ticket type");
        }
        if (paymentReference == null || paymentReference.isBlank()) {
            throw new IllegalArgumentException("paymentReference must not be blank");
        }
        this.id = id;
        this.reservationId = reservationId;
        this.eventId = eventId;
        this.customerId = customerId;
        this.lines = lines;
        this.total = total;
        this.paymentReference = paymentReference;
        this.paidAt = paidAt;
    }

    public static Order from(Reservation reservation, String paymentReference, Instant paidAt) {
        return new Order(OrderId.newId(), reservation.getId(), reservation.getEventId(), reservation.getCustomerId(),
                reservation.getLines(), reservation.total(), paymentReference, paidAt);
    }
}
