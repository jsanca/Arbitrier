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
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class OrderPersistenceMapperTest {

    private static final OrderPersistenceMapper MAPPER = new OrderPersistenceMapper();

    private static final OrderId ORDER_ID = OrderId.of("order-1");
    private static final CustomerId CUSTOMER_ID = CustomerId.of("cust-1");
    private static final UserId USER_ID = UserId.of("user-1");
    private static final List<OrderLine> LINES = List.of(
            new OrderLine(Sku.of("SKU-A"), Quantity.of(5)),
            new OrderLine(Sku.of("SKU-B"), Quantity.of(3)));

    // ── toEntity / new order ──────────────────────────────────────────────────

    @Test
    void new_order_maps_to_entity_with_null_version() {
        Order order = Order.create(ORDER_ID, CUSTOMER_ID, USER_ID, LINES);

        OrderEntity entity = MAPPER.toEntity(order);

        assertThat(entity.getId()).isEqualTo("order-1");
        assertThat(entity.getCustomerId()).isEqualTo("cust-1");
        assertThat(entity.getSubmittedBy()).isEqualTo("user-1");
        assertThat(entity.getStatus()).isEqualTo("PENDING");
        assertThat(entity.getCancellationReason()).isNull();
        assertThat(entity.getVersion()).isNull();
        assertThat(entity.getLines()).hasSize(2);
    }

    @Test
    void cancelled_order_maps_cancellation_reason_to_entity() {
        Order order = Order.create(ORDER_ID, CUSTOMER_ID, USER_ID, LINES)
                .cancel(CancellationReason.INSUFFICIENT_CREDIT);

        OrderEntity entity = MAPPER.toEntity(order);

        assertThat(entity.getStatus()).isEqualTo("CANCELLED");
        assertThat(entity.getCancellationReason()).isEqualTo("INSUFFICIENT_CREDIT");
    }

    @Test
    void toEntity_maps_lines_with_parent_reference() {
        Order order = Order.create(ORDER_ID, CUSTOMER_ID, USER_ID, LINES);

        OrderEntity entity = MAPPER.toEntity(order);

        assertThat(entity.getLines()).allMatch(line -> line.getOrder() == entity);
        assertThat(entity.getLines()).extracting(OrderLineEntity::getSku)
                .containsExactlyInAnyOrder("SKU-A", "SKU-B");
        assertThat(entity.getLines()).extracting(OrderLineEntity::getQuantity)
                .containsExactlyInAnyOrder(5, 3);
    }

    // ── toDomain ─────────────────────────────────────────────────────────────

    @Test
    void entity_round_trips_to_domain_and_back() {
        Order original = Order.reconstruct(ORDER_ID, CUSTOMER_ID, USER_ID, LINES,
                OrderStatus.CONFIRMED, null, 7L);

        OrderEntity entity = MAPPER.toEntity(original);
        Order restored = MAPPER.toDomain(entity);

        assertThat(restored.id()).isEqualTo(ORDER_ID);
        assertThat(restored.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(restored.submittedBy()).isEqualTo(USER_ID);
        assertThat(restored.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(restored.cancellationReason()).isNull();
        assertThat(restored.version()).isEqualTo(7L);
        assertThat(restored.lines()).hasSize(2);
    }

    @Test
    void toDomain_restores_cancellation_reason() {
        Order original = Order.reconstruct(ORDER_ID, CUSTOMER_ID, USER_ID, LINES,
                OrderStatus.CANCELLED, CancellationReason.CUSTOMER_CANCELLED, 3L);

        Order restored = MAPPER.toDomain(MAPPER.toEntity(original));

        assertThat(restored.cancellationReason()).isEqualTo(CancellationReason.CUSTOMER_CANCELLED);
    }

    @Test
    void toDomain_restores_version() {
        Order original = Order.reconstruct(ORDER_ID, CUSTOMER_ID, USER_ID, LINES,
                OrderStatus.PENDING, null, 42L);

        Order restored = MAPPER.toDomain(MAPPER.toEntity(original));

        assertThat(restored.version()).isEqualTo(42L);
    }

    // ── updateEntity ─────────────────────────────────────────────────────────

    @Test
    void updateEntity_replaces_lines_and_preserves_entity_version() {
        Order original = Order.create(ORDER_ID, CUSTOMER_ID, USER_ID, LINES);
        OrderEntity entity = MAPPER.toEntity(original);
        entity.setVersion(5L);

        List<OrderLine> newLines = List.of(new OrderLine(Sku.of("SKU-C"), Quantity.of(1)));
        Order updated = Order.reconstruct(ORDER_ID, CUSTOMER_ID, USER_ID, newLines,
                OrderStatus.CONFIRMED, null, 5L);

        MAPPER.updateEntity(entity, updated);

        assertThat(entity.getVersion()).isEqualTo(5L);
        assertThat(entity.getStatus()).isEqualTo("CONFIRMED");
        assertThat(entity.getLines()).hasSize(1);
        assertThat(entity.getLines().get(0).getSku()).isEqualTo("SKU-C");
    }

    // ── corrupt data ─────────────────────────────────────────────────────────

    @Test
    void toDomain_rejects_unknown_status() {
        OrderEntity entity = buildEntity("UNKNOWN_STATUS", null);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> MAPPER.toDomain(entity))
                .withMessageContaining("UNKNOWN_STATUS");
    }

    @Test
    void toDomain_rejects_unknown_cancellation_reason() {
        OrderEntity entity = buildEntity("CANCELLED", "UNKNOWN_REASON");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> MAPPER.toDomain(entity))
                .withMessageContaining("UNKNOWN_REASON");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static OrderEntity buildEntity(String status, String cancellationReason) {
        OrderEntity entity = new OrderEntity();
        entity.setId("order-1");
        entity.setCustomerId("cust-1");
        entity.setSubmittedBy("user-1");
        entity.setStatus(status);
        entity.setCancellationReason(cancellationReason);
        entity.setVersion(1L);

        OrderLineEntity line = new OrderLineEntity();
        line.setOrder(entity);
        line.setSku("SKU-A");
        line.setQuantity(1);
        entity.getLines().add(line);

        return entity;
    }
}
