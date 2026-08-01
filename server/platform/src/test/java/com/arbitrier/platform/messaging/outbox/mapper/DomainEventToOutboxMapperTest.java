package com.arbitrier.platform.messaging.outbox.mapper;

import com.arbitrier.platform.messaging.event.DomainEvent;
import com.arbitrier.platform.messaging.event.EventDescriptor;
import com.arbitrier.platform.messaging.outbox.MessageNature;
import com.arbitrier.platform.messaging.outbox.OutboxEvent;
import com.arbitrier.platform.messaging.outbox.PublishStatus;
import com.arbitrier.platform.messaging.serialization.JacksonEventSerializer;
import com.arbitrier.platform.time.FixedTimeProvider;
import com.arbitrier.platform.time.TimeProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class DomainEventToOutboxMapperTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    private final TimeProvider clock = FixedTimeProvider.of(NOW);
    private DomainEventToOutboxMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new DomainEventToOutboxMapper(
                new JacksonEventSerializer(new ObjectMapper()), clock);
    }

    @Test
    void maps_domain_event_to_pending_outbox_event() {
        OrderPlacedEvent domainEvent = new OrderPlacedEvent("order-1", "cust-a");
        OutboxEvent result = mapper.map(domainEvent, "order-1", "Order");

        assertThat(result.eventId()).isNotNull();
        assertThat(result.aggregateId()).isEqualTo("order-1");
        assertThat(result.aggregateType()).isEqualTo("Order");
        assertThat(result.eventType()).isEqualTo("OrderPlacedEvent");
        assertThat(result.payloadFormat()).isEqualTo("JSON");
        assertThat(result.occurredAt()).isEqualTo(NOW);
        assertThat(result.publishStatus()).isEqualTo(PublishStatus.PENDING);
        assertThat(result.attemptCount()).isZero();
        assertThat(result.publishedAt()).isNull();
        assertThat(result.lastAttempt()).isNull();
        assertThat(result.correlationId()).isNull();
        assertThat(result.causationId()).isNull();
        assertThat(result.messageNature()).isEqualTo(MessageNature.EVENT);
    }

    @Test
    void three_arg_overload_always_produces_event_nature() {
        OutboxEvent result = mapper.map(new OrderPlacedEvent("o", "c"), "o", "Order");
        assertThat(result.messageNature()).isEqualTo(MessageNature.EVENT);
    }

    @Test
    void four_arg_overload_maps_command_nature() {
        ReserveStockCommand command = new ReserveStockCommand("order-1", "sku-A", 5);
        OutboxEvent result = mapper.map(command, "order-1", "Saga", MessageNature.COMMAND);

        assertThat(result.messageNature()).isEqualTo(MessageNature.COMMAND);
        assertThat(result.eventType()).isEqualTo("ReserveStockCommand");
        assertThat(result.publishStatus()).isEqualTo(PublishStatus.PENDING);
    }

    @Test
    void four_arg_null_nature_throws() {
        assertThatNullPointerException()
                .isThrownBy(() -> mapper.map(new OrderPlacedEvent("x", "y"), "x", "Order", null))
                .withMessageContaining("nature");
    }

    @Test
    void payload_contains_serialized_event_data() {
        OrderPlacedEvent event = new OrderPlacedEvent("order-9", "cust-z");
        OutboxEvent result = mapper.map(event, "order-9", "Order");

        assertThat(result.payload()).contains("\"orderId\"")
                .contains("\"order-9\"")
                .contains("\"customerId\"")
                .contains("\"cust-z\"");
    }

    @Test
    void falls_back_to_class_simple_name_when_domain_event_not_implemented() {
        // OrderPlacedEvent is a plain record — no DomainEvent interface
        OutboxEvent result = mapper.map(new OrderPlacedEvent("x", "y"), "x", "Order");
        assertThat(result.eventType()).isEqualTo("OrderPlacedEvent");
    }

    @Test
    void uses_descriptor_type_when_domain_event_interface_implemented() {
        // DomainEventAwareOrderPlaced explicitly declares a stable logical type
        OutboxEvent result = mapper.map(new DomainEventAwareOrderPlaced("x", "y"), "x", "Order");
        assertThat(result.eventType()).isEqualTo("order.placed");
    }

    @Test
    void descriptor_type_is_independent_of_java_class_name() {
        // Renaming the Java class would change getSimpleName() but not descriptor().type()
        DomainEventAwareOrderPlaced event = new DomainEventAwareOrderPlaced("id", "cust");
        assertThat(event.descriptor().type()).isNotEqualTo(event.getClass().getSimpleName());
        assertThat(event.descriptor().type()).isEqualTo("order.placed");
    }

    @Test
    void generates_unique_event_ids() {
        OutboxEvent e1 = mapper.map(new OrderPlacedEvent("a", "b"), "a", "Order");
        OutboxEvent e2 = mapper.map(new OrderPlacedEvent("a", "b"), "a", "Order");
        assertThat(e1.eventId()).isNotEqualTo(e2.eventId());
    }

    @Test
    void null_message_throws() {
        assertThatNullPointerException()
                .isThrownBy(() -> mapper.map(null, "id", "Order"))
                .withMessageContaining("message");
    }

    @Test
    void blank_aggregateId_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> mapper.map(new OrderPlacedEvent("x", "y"), "  ", "Order"))
                .withMessageContaining("aggregateId");
    }

    @Test
    void blank_aggregateType_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> mapper.map(new OrderPlacedEvent("x", "y"), "id", ""))
                .withMessageContaining("aggregateType");
    }

    @Test
    void constructor_rejects_null_serializer() {
        assertThatNullPointerException()
                .isThrownBy(() -> new DomainEventToOutboxMapper(null, clock))
                .withMessageContaining("serializer");
    }

    @Test
    void constructor_rejects_null_timeProvider() {
        assertThatNullPointerException()
                .isThrownBy(() -> new DomainEventToOutboxMapper(
                        new JacksonEventSerializer(new ObjectMapper()), null))
                .withMessageContaining("timeProvider");
    }

    public record OrderPlacedEvent(String orderId, String customerId) {}

    public record ReserveStockCommand(String orderId, String sku, int quantity) {}

    /** Test event that implements DomainEvent with a stable logical type. */
    public record DomainEventAwareOrderPlaced(String orderId, String customerId)
            implements DomainEvent {
        @Override
        public EventDescriptor descriptor() {
            return new EventDescriptor("order.placed", 1);
        }
    }
}
