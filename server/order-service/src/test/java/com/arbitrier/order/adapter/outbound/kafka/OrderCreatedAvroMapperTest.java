package com.arbitrier.order.adapter.outbound.kafka;

import com.arbitrier.contracts.common.OrderLineContract;
import com.arbitrier.contracts.order.OrderCreated;
import com.arbitrier.order.domain.event.OrderCreatedDomainEvent;
import com.arbitrier.order.domain.model.CustomerId;
import com.arbitrier.order.domain.model.OrderId;
import com.arbitrier.order.domain.model.OrderLine;
import com.arbitrier.order.domain.model.Quantity;
import com.arbitrier.order.domain.model.Sku;
import com.arbitrier.order.domain.model.UserId;
import com.arbitrier.platform.time.FixedTimeProvider;
import com.arbitrier.platform.time.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OrderCreatedAvroMapper}.
 *
 * <p>Verifies field mapping from domain event to Avro contract.
 * No Spring context or Kafka broker required.
 */
class OrderCreatedAvroMapperTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-07-08T10:00:00Z");

    private OrderCreatedAvroMapper mapper;

    @BeforeEach
    void setUp() {
        TimeProvider timeProvider = FixedTimeProvider.of(FIXED_INSTANT);
        mapper = new OrderCreatedAvroMapper(timeProvider);
    }

    @Test
    void maps_orderId_to_avro() {
        OrderCreatedDomainEvent event = buildEvent("order-001", "cust-1", "user-1",
                List.of(new OrderLine(new Sku("SKU-A"), new Quantity(5))));
        String correlationId = UUID.randomUUID().toString();

        OrderCreated avro = mapper.map(event, correlationId);

        assertThat(avro.getOrderId()).isEqualTo("order-001");
    }

    @Test
    void maps_customerId_to_avro() {
        OrderCreatedDomainEvent event = buildEvent("order-001", "cust-42", "user-1",
                List.of(new OrderLine(new Sku("SKU-A"), new Quantity(1))));

        OrderCreated avro = mapper.map(event, "corr-id");

        assertThat(avro.getCustomerId()).isEqualTo("cust-42");
    }

    @Test
    void maps_submittedByUserId_to_avro() {
        OrderCreatedDomainEvent event = buildEvent("order-001", "cust-1", "user-99",
                List.of(new OrderLine(new Sku("SKU-A"), new Quantity(1))));

        OrderCreated avro = mapper.map(event, "corr-id");

        assertThat(avro.getSubmittedByUserId()).isEqualTo("user-99");
    }

    @Test
    void maps_lines_to_avro() {
        List<OrderLine> lines = List.of(
                new OrderLine(new Sku("SKU-A"), new Quantity(10)),
                new OrderLine(new Sku("SKU-B"), new Quantity(3)));
        OrderCreatedDomainEvent event = buildEvent("order-001", "cust-1", "user-1", lines);

        OrderCreated avro = mapper.map(event, "corr-id");

        assertThat(avro.getLines()).hasSize(2);
        OrderLineContract first = avro.getLines().get(0);
        assertThat(first.getSku()).isEqualTo("SKU-A");
        assertThat(first.getQuantity()).isEqualTo(10);
        OrderLineContract second = avro.getLines().get(1);
        assertThat(second.getSku()).isEqualTo("SKU-B");
        assertThat(second.getQuantity()).isEqualTo(3);
    }

    @Test
    void creates_metadata_with_non_null_messageId() {
        OrderCreatedDomainEvent event = buildEvent("order-001", "cust-1", "user-1",
                List.of(new OrderLine(new Sku("SKU-A"), new Quantity(1))));

        OrderCreated avro = mapper.map(event, "corr-id");

        assertThat(avro.getMetadata().getMessageId()).isNotBlank();
        // messageId must be a valid UUID
        assertThat(UUID.fromString(avro.getMetadata().getMessageId())).isNotNull();
    }

    @Test
    void creates_metadata_with_supplied_correlationId() {
        String correlationId = "test-correlation-xyz";
        OrderCreatedDomainEvent event = buildEvent("order-001", "cust-1", "user-1",
                List.of(new OrderLine(new Sku("SKU-A"), new Quantity(1))));

        OrderCreated avro = mapper.map(event, correlationId);

        assertThat(avro.getMetadata().getCorrelationId()).isEqualTo(correlationId);
    }

    @Test
    void creates_metadata_with_null_causationId_for_root_message() {
        OrderCreatedDomainEvent event = buildEvent("order-001", "cust-1", "user-1",
                List.of(new OrderLine(new Sku("SKU-A"), new Quantity(1))));

        OrderCreated avro = mapper.map(event, "corr-id");

        assertThat(avro.getMetadata().getCausationId()).isNull();
    }

    @Test
    void creates_metadata_with_occurredAt_from_time_provider() {
        OrderCreatedDomainEvent event = buildEvent("order-001", "cust-1", "user-1",
                List.of(new OrderLine(new Sku("SKU-A"), new Quantity(1))));

        OrderCreated avro = mapper.map(event, "corr-id");

        assertThat(avro.getMetadata().getOccurredAt()).isEqualTo(FIXED_INSTANT.toString());
    }

    @Test
    void creates_metadata_with_schema_version_v1() {
        OrderCreatedDomainEvent event = buildEvent("order-001", "cust-1", "user-1",
                List.of(new OrderLine(new Sku("SKU-A"), new Quantity(1))));

        OrderCreated avro = mapper.map(event, "corr-id");

        assertThat(avro.getMetadata().getSchemaVersion()).isEqualTo("v1");
    }

    @Test
    void requestedTotal_is_zero_usd_placeholder() {
        // ARB-011 decision: requestedTotal uses MoneyAmount("0","USD") until pricing
        // source of truth is resolved. Downstream consumers must not rely on this value.
        OrderCreatedDomainEvent event = buildEvent("order-001", "cust-1", "user-1",
                List.of(new OrderLine(new Sku("SKU-A"), new Quantity(1))));

        OrderCreated avro = mapper.map(event, "corr-id");

        assertThat(avro.getRequestedTotal().getAmount()).isEqualTo("0");
        assertThat(avro.getRequestedTotal().getCurrency()).isEqualTo("USD");
    }

    private OrderCreatedDomainEvent buildEvent(
            String orderId, String customerId, String userId, List<OrderLine> lines) {
        return new OrderCreatedDomainEvent(
                new OrderId(orderId),
                new CustomerId(customerId),
                new UserId(userId),
                lines);
    }
}
