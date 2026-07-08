package com.arbitrier.order.adapter.outbound;

import com.arbitrier.order.application.port.outbound.OrderRepository;
import com.arbitrier.order.domain.model.Order;
import com.arbitrier.order.domain.model.OrderId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory {@link OrderRepository} for unit tests and local slices.
 */
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<String, Order> store = new ConcurrentHashMap<>();

    @Override
    public void save(Order order) {
        store.put(order.id().value(), order);
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return Optional.ofNullable(store.get(id.value()));
    }

    /** Returns all saved orders (test helper). */
    public List<Order> findAll() {
        return new ArrayList<>(store.values());
    }

    /** Returns the number of saved orders (test helper). */
    public int size() {
        return store.size();
    }
}
