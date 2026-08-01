package com.arbitrier.order.adapter.outbound.transport.model;

import com.arbitrier.order.domain.event.OrderCreatedDomainEvent;
import com.arbitrier.order.domain.model.CustomerId;
import com.arbitrier.order.domain.model.OrderId;
import com.arbitrier.order.domain.model.OrderLine;
import com.arbitrier.order.domain.model.Quantity;
import com.arbitrier.order.domain.model.Sku;
import com.arbitrier.order.domain.model.UserId;
import com.arbitrier.platform.messaging.event.EventDescriptor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Unit tests for {@link TransportEventMetadata}.
 *
 * <p>Proves that transport identity is stable, derived correctly from
 * {@link EventDescriptor}, and independent of Java class names.
 *
 * <p>Layer: adapter/outbound/transport/model
 * <p>Module: order-service
 */
class TransportEventMetadataTest {

    // ── EventDescriptor stability ─────────────────────────────────────────────

    @Test
    void order_created_domain_event_has_stable_descriptor_type() {
        OrderCreatedDomainEvent event = buildOrderCreatedEvent();
        assertThat(event.descriptor().type()).isEqualTo("order.created");
    }

    @Test
    void order_created_domain_event_has_version_1() {
        OrderCreatedDomainEvent event = buildOrderCreatedEvent();
        assertThat(event.descriptor().version()).isEqualTo(1);
    }

    @Test
    void descriptor_type_is_independent_of_java_class_name() {
        OrderCreatedDomainEvent event = buildOrderCreatedEvent();
        assertThat(event.descriptor().type()).isNotEqualTo(event.getClass().getSimpleName());
        assertThat(event.descriptor().type()).isEqualTo("order.created");
    }

    // ── TransportEventMetadata derivation ────────────────────────────────────

    @Test
    void from_descriptor_produces_correct_event_type() {
        EventDescriptor descriptor = new EventDescriptor("order.created", 1);
        TransportEventMetadata meta = TransportEventMetadata.from(descriptor);
        assertThat(meta.eventType()).isEqualTo("order.created");
    }

    @Test
    void from_descriptor_converts_integer_version_to_string() {
        EventDescriptor descriptor = new EventDescriptor("order.created", 1);
        TransportEventMetadata meta = TransportEventMetadata.from(descriptor);
        assertThat(meta.eventVersion()).isEqualTo("v1");
    }

    @Test
    void from_descriptor_version_2_becomes_v2() {
        EventDescriptor descriptor = new EventDescriptor("order.updated", 2);
        TransportEventMetadata meta = TransportEventMetadata.from(descriptor);
        assertThat(meta.eventVersion()).isEqualTo("v2");
    }

    @Test
    void from_order_created_domain_event_produces_expected_metadata() {
        TransportEventMetadata meta = TransportEventMetadata.from(
                buildOrderCreatedEvent().descriptor());
        assertThat(meta.eventType()).isEqualTo("order.created");
        assertThat(meta.eventVersion()).isEqualTo("v1");
    }

    // ── invariant enforcement ─────────────────────────────────────────────────

    @Test
    void event_descriptor_rejects_blank_type() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new EventDescriptor("", 1))
                .withMessageContaining("EventDescriptor.type");
    }

    @Test
    void event_descriptor_rejects_version_zero() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new EventDescriptor("order.created", 0))
                .withMessageContaining("version");
    }

    @Test
    void event_descriptor_rejects_negative_version() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new EventDescriptor("order.created", -1))
                .withMessageContaining("version");
    }

    @Test
    void transport_metadata_rejects_null_descriptor() {
        assertThatNullPointerException()
                .isThrownBy(() -> TransportEventMetadata.from(null))
                .withMessageContaining("descriptor");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static OrderCreatedDomainEvent buildOrderCreatedEvent() {
        return new OrderCreatedDomainEvent(
                new OrderId("order-001"),
                new CustomerId("cust-001"),
                new UserId("user-001"),
                List.of(new OrderLine(new Sku("SKU-A"), new Quantity(5))));
    }
}
