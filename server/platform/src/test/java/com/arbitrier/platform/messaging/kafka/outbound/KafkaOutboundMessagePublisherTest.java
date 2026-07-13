package com.arbitrier.platform.messaging.kafka.outbound;

import com.arbitrier.platform.kafka.KafkaHeaders;
import com.arbitrier.platform.messaging.outbox.MessageNature;
import com.arbitrier.platform.messaging.outbox.OutboxEvent;
import com.arbitrier.platform.messaging.outbox.OutboundPayloadSerializer;
import com.arbitrier.platform.messaging.outbox.OutboundRoutingStrategy;
import com.arbitrier.platform.messaging.outbox.PublishStatus;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import org.springframework.kafka.support.SendResult;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link KafkaOutboundMessagePublisher}.
 * No broker, no Testcontainers — KafkaTemplate and OutboundRoutingStrategy are mocked.
 */
@ExtendWith(MockitoExtension.class)
class KafkaOutboundMessagePublisherTest {

    @Mock
    KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    OutboundRoutingStrategy routingStrategy;

    @Mock
    OutboundPayloadSerializer payloadSerializer;

    private KafkaOutboundMessagePublisher publisher;

    private static final String DESTINATION = "arbitrier.order.created.v1";
    private static final String AGGREGATE_ID = "order-test-001";
    private static final String CORRELATION_ID = "corr-001";
    private static final String CAUSATION_ID = "cause-001";
    private static final String SERIALIZED_PAYLOAD = "{\"orderId\":\"order-test-001\"}";

    @BeforeEach
    void setUp() {
        publisher = new KafkaOutboundMessagePublisher(kafkaTemplate, routingStrategy, payloadSerializer);
        lenient().when(routingStrategy.resolveDestination(any())).thenReturn(DESTINATION);
        lenient().when(payloadSerializer.serialize(any())).thenReturn(SERIALIZED_PAYLOAD);
        lenient().when(kafkaTemplate.send(any(ProducerRecord.class)))
                 .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void resolves_destination_through_routing_strategy() {
        publisher.publish(eventMessage());

        verify(routingStrategy).resolveDestination(any());
        ArgumentCaptor<ProducerRecord<String, String>> captor = recordCaptor();
        verify(kafkaTemplate).send(captor.capture());
        assertThat(captor.getValue().topic()).isEqualTo(DESTINATION);
    }

    @Test
    void publishes_payload_serializer_output() {
        publisher.publish(eventMessage());

        ArgumentCaptor<ProducerRecord<String, String>> captor = recordCaptor();
        verify(kafkaTemplate).send(captor.capture());
        assertThat(captor.getValue().value()).isEqualTo(SERIALIZED_PAYLOAD);
    }

    @Test
    void delegates_serialization_to_payload_serializer() {
        OutboxEvent event = eventMessage();
        publisher.publish(event);

        verify(payloadSerializer).serialize(event);
    }

    @Test
    void successful_kafka_send_completes_stage_normally() {
        CompletableFuture<SendResult<String, String>> sent = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(sent);

        CompletableFuture<Void> result = publisher.publish(eventMessage()).toCompletableFuture();

        assertThat(result.isDone()).isTrue();
        assertThat(result.isCompletedExceptionally()).isFalse();
    }

    @Test
    void failed_kafka_send_completes_stage_exceptionally() {
        CompletableFuture<SendResult<String, String>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("broker unavailable"));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(failed);

        CompletableFuture<Void> result = publisher.publish(eventMessage()).toCompletableFuture();

        assertThat(result.isCompletedExceptionally()).isTrue();
    }

    @Test
    void uses_aggregateId_as_partition_key() {
        publisher.publish(eventMessage());

        ArgumentCaptor<ProducerRecord<String, String>> captor = recordCaptor();
        verify(kafkaTemplate).send(captor.capture());
        assertThat(captor.getValue().key()).isEqualTo(AGGREGATE_ID);
    }

    @Test
    void propagates_event_nature_metadata_headers() {
        OutboxEvent event = eventMessage();
        publisher.publish(event);

        ArgumentCaptor<ProducerRecord<String, String>> captor = recordCaptor();
        verify(kafkaTemplate).send(captor.capture());
        ProducerRecord<String, String> record = captor.getValue();

        assertThat(header(record, KafkaHeaders.MESSAGE_ID)).isEqualTo(event.eventId().toString());
        assertThat(header(record, KafkaHeaders.EVENT_TYPE)).isEqualTo("OrderCreatedDomainEvent");
        assertThat(header(record, KafkaHeaders.MESSAGE_NATURE)).isEqualTo("EVENT");
        assertThat(header(record, KafkaHeaders.AGGREGATE_ID)).isEqualTo(AGGREGATE_ID);
        assertThat(header(record, KafkaHeaders.AGGREGATE_TYPE)).isEqualTo("Order");
        assertThat(header(record, KafkaHeaders.PAYLOAD_FORMAT)).isEqualTo("JSON");
        assertThat(header(record, KafkaHeaders.CORRELATION_ID)).isEqualTo(CORRELATION_ID);
        assertThat(header(record, KafkaHeaders.CAUSATION_ID)).isEqualTo(CAUSATION_ID);
    }

    @Test
    void propagates_command_nature_metadata_headers() {
        OutboxEvent command = commandMessage();
        publisher.publish(command);

        ArgumentCaptor<ProducerRecord<String, String>> captor = recordCaptor();
        verify(kafkaTemplate).send(captor.capture());
        assertThat(header(captor.getValue(), KafkaHeaders.MESSAGE_NATURE)).isEqualTo("COMMAND");
        assertThat(header(captor.getValue(), KafkaHeaders.EVENT_TYPE)).isEqualTo("ReserveStockCommand");
    }

    @Test
    void omits_null_correlation_and_causation_headers() {
        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(), AGGREGATE_ID, "Order", "OrderCreatedDomainEvent",
                "{}", "JSON", Instant.now(), null, PublishStatus.PENDING, 0, null,
                null, null, MessageNature.EVENT);

        publisher.publish(event);

        ArgumentCaptor<ProducerRecord<String, String>> captor = recordCaptor();
        verify(kafkaTemplate).send(captor.capture());
        assertThat(captor.getValue().headers().lastHeader(KafkaHeaders.CORRELATION_ID)).isNull();
        assertThat(captor.getValue().headers().lastHeader(KafkaHeaders.CAUSATION_ID)).isNull();
    }

    @Test
    void routing_strategy_failure_propagates_and_send_is_never_called() {
        when(routingStrategy.resolveDestination(any()))
                .thenThrow(new IllegalStateException("no route for event type"));

        assertThatThrownBy(() -> publisher.publish(eventMessage()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no route for event type");

        verify(kafkaTemplate, never()).send(any(ProducerRecord.class));
    }

    @Test
    void rejects_null_outbox_event() {
        assertThatNullPointerException()
                .isThrownBy(() -> publisher.publish(null))
                .withMessageContaining("message");
    }

    @Test
    void invokes_kafka_template_exactly_once() {
        publisher.publish(eventMessage());
        verify(kafkaTemplate, times(1)).send(any(ProducerRecord.class));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<ProducerRecord<String, String>> recordCaptor() {
        return ArgumentCaptor.forClass((Class<ProducerRecord<String, String>>) (Class<?>) ProducerRecord.class);
    }

    private String header(ProducerRecord<String, String> record, String name) {
        Header h = record.headers().lastHeader(name);
        assertThat(h).as("header '%s'", name).isNotNull();
        return new String(h.value(), StandardCharsets.UTF_8);
    }

    private OutboxEvent eventMessage() {
        return new OutboxEvent(
                UUID.randomUUID(), AGGREGATE_ID, "Order", "OrderCreatedDomainEvent",
                "{\"orderId\":\"order-test-001\"}", "JSON",
                Instant.now(), null, PublishStatus.PENDING, 0, null,
                CORRELATION_ID, CAUSATION_ID, MessageNature.EVENT);
    }

    private OutboxEvent commandMessage() {
        return new OutboxEvent(
                UUID.randomUUID(), "saga-001", "Saga", "ReserveStockCommand",
                "{\"sku\":\"SKU-A\"}", "JSON",
                Instant.now(), null, PublishStatus.PENDING, 0, null,
                CORRELATION_ID, null, MessageNature.COMMAND);
    }
}
