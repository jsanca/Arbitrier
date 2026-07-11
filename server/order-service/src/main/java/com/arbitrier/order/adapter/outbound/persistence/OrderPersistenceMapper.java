package com.arbitrier.order.adapter.outbound.persistence;

import com.arbitrier.order.domain.model.CancellationReason;
import com.arbitrier.order.domain.model.CustomerId;
import com.arbitrier.order.domain.model.Order;
import com.arbitrier.order.domain.model.OrderId;
import com.arbitrier.order.domain.model.OrderLine;
import com.arbitrier.order.domain.model.OrderStatus;
import com.arbitrier.order.domain.model.Quantity;
import com.arbitrier.order.domain.model.Sku;
import com.arbitrier.order.domain.model.UserId;
import com.arbitrier.platform.validation.Require;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps between {@link Order} domain aggregates and {@link OrderEntity} JPA entities.
 *
 * <p>Reconstruction from entity is the authoritative path for loading aggregates from
 * the database. All domain invariant validation runs through the domain constructors.
 *
 * <p>Layer: adapter/outbound/persistence
 * <p>Module: order-service
 */
public class OrderPersistenceMapper {

    /**
     * Creates a new {@link OrderEntity} from a domain {@link Order}.
     * The entity's {@code version} is set from the domain object (null for new orders).
     */
    public OrderEntity toEntity(Order order) {
        OrderEntity entity = new OrderEntity();
        entity.setId(order.id().value());
        entity.setCustomerId(order.customerId().value());
        entity.setSubmittedBy(order.submittedBy().value());
        entity.setStatus(order.status().name());
        entity.setCancellationReason(
                order.cancellationReason() != null ? order.cancellationReason().name() : null);
        entity.setVersion(order.version());

        List<OrderLineEntity> lineEntities = order.lines().stream()
                .map(line -> toLineEntity(line, entity))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        entity.setLines(lineEntities);

        return entity;
    }

    /**
     * Updates an existing managed {@link OrderEntity} with state from the domain {@link Order}.
     *
     * <p>Clears and replaces lines atomically. The entity's {@code version} is preserved
     * from the managed entity (not overwritten from domain) to allow JPA optimistic locking.
     */
    public OrderEntity updateEntity(OrderEntity existing, Order order) {
        existing.setCustomerId(order.customerId().value());
        existing.setSubmittedBy(order.submittedBy().value());
        existing.setStatus(order.status().name());
        existing.setCancellationReason(
                order.cancellationReason() != null ? order.cancellationReason().name() : null);

        existing.getLines().clear();
        order.lines().forEach(line -> {
            OrderLineEntity lineEntity = toLineEntity(line, existing);
            existing.getLines().add(lineEntity);
        });

        return existing;
    }

    /**
     * Reconstructs a domain {@link Order} from a {@link OrderEntity}.
     *
     * @throws IllegalArgumentException if the entity contains unrecognised status or reason values
     */
    public Order toDomain(OrderEntity entity) {
        Require.notNull(entity, "OrderEntity");

        OrderStatus status = parseStatus(entity.getStatus());
        CancellationReason reason = parseCancellationReason(entity.getCancellationReason());
        List<OrderLine> lines = entity.getLines().stream()
                .map(this::toOrderLine)
                .toList();

        return Order.reconstruct(
                OrderId.of(entity.getId()),
                CustomerId.of(entity.getCustomerId()),
                UserId.of(entity.getSubmittedBy()),
                lines,
                status,
                reason,
                entity.getVersion());
    }

    private OrderLineEntity toLineEntity(OrderLine line, OrderEntity parent) {
        OrderLineEntity lineEntity = new OrderLineEntity();
        lineEntity.setOrder(parent);
        lineEntity.setSku(line.sku().value());
        lineEntity.setQuantity(line.quantity().value());
        return lineEntity;
    }

    private OrderLine toOrderLine(OrderLineEntity entity) {
        return new OrderLine(
                Sku.of(entity.getSku()),
                Quantity.of(entity.getQuantity()));
    }

    private static OrderStatus parseStatus(String value) {
        try {
            return OrderStatus.valueOf(Require.notBlank(value, "OrderEntity.status"));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unrecognised OrderStatus in persisted data: '" + value + "'", e);
        }
    }

    private static CancellationReason parseCancellationReason(String value) {
        if (value == null) {
            return null;
        }
        try {
            return CancellationReason.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unrecognised CancellationReason in persisted data: '" + value + "'", e);
        }
    }
}
