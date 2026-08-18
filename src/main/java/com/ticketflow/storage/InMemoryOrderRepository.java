package com.ticketflow.storage;

import com.ticketflow.domain.Order;
import com.ticketflow.domain.id.OrderId;
import io.vavr.collection.List;
import io.vavr.control.Option;
import java.util.LinkedHashMap;
import java.util.Map;

public final class InMemoryOrderRepository implements OrderRepository {
    private final Map<OrderId, Order> orders = new LinkedHashMap<>();
    private final Map<String, OrderId> byPaymentReference = new LinkedHashMap<>();

    @Override
    public Option<Order> findById(OrderId id) {
        return Option.of(orders.get(id));
    }

    @Override
    public Order save(Order entity) {
        Order previous = orders.put(entity.getId(), entity);
        if (previous != null && !previous.getPaymentReference().equals(entity.getPaymentReference())) {
            byPaymentReference.remove(previous.getPaymentReference());
        }
        byPaymentReference.put(entity.getPaymentReference(), entity.getId());
        return entity;
    }

    @Override
    public List<Order> findAll() {
        return List.ofAll(orders.values());
    }

    @Override
    public Option<Order> findByPaymentReference(String paymentReference) {
        return Option.of(byPaymentReference.get(paymentReference)).flatMap(this::findById);
    }
}
