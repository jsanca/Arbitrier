package com.arbitrier.order.application.port.outbound;

import com.arbitrier.order.domain.model.Order;
import com.arbitrier.order.domain.model.OrderId;
import java.util.Optional;

/**
 * Outbound port: persistence operations for {@link Order}.
 *
 * <p>Layer: application/port/outbound
 * <p>Module: order-service
 */
public interface OrderRepository {

    /** Persists a new or updated order. */
    void save(Order order);

    /** Looks up an order by its identifier. */
    Optional<Order> findById(OrderId id);
}
