package com.ticketflow.storage;

import com.ticketflow.domain.Order;
import com.ticketflow.domain.id.OrderId;
import io.vavr.control.Option;

public interface OrderRepository extends Repository<Order, OrderId> {
    Option<Order> findByPaymentReference(String paymentReference);
}
