package com.arbitrier.platform.messaging.kafka.outbound;

import com.arbitrier.platform.kafka.KafkaHeaders;
import com.arbitrier.platform.messaging.outbox.OutboxEvent;
import com.arbitrier.platform.messaging.outbox.OutboundMessagePublisher;
import com.arbitrier.platform.messaging.outbox.OutboundPayloadSerializer;
import com.arbitrier.platform.messaging.outbox.OutboundRoutingStrategy;
import com.arbitrier.platform.validation.Require;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionStage;

/**
 * Kafka adapter implementing {@link OutboundMessagePublisher}.
 *
 * <p>Resolves the transport destination via {@link OutboundRoutingStrategy} and serializes
 * the payload via {@link OutboundPayloadSerializer}, then publishes the record to Kafka.
 *
 * <h2>Key strategy</h2>
 * <p>{@link OutboxEvent#aggregateId()} is used as the Kafka partition key so that all messages
 * for the same aggregate are ordered within a partition.
 *
 * <h2>Headers propagated</h2>
 * <p>All non-null metadata from the {@code OutboxEvent} is forwarded as UTF-8 string headers:
 * {@code messageId}, {@code eventType}, {@code messageNature}, {@code aggregateId},
 * {@code aggregateType}, {@code payloadFormat}, {@code correlationId} (if non-null),
 * {@code causationId} (if non-null).
 *
 * <h2>Asynchronous delivery</h2>
 * <p>{@link KafkaTemplate#send} is non-blocking. This adapter initiates publication and returns
 * immediately; it does not call {@code OutboxRepository.markPublished()} or
 * {@code markFailed()}. Those calls belong to the outbox drainer (a future slice) which owns
 * delivery acknowledgement.
 *
 * <p>Layer: platform/messaging/kafka/outbound
 * <p>Module: platform
 */
public class KafkaOutboundMessagePublisher implements OutboundMessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaOutboundMessagePublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboundRoutingStrategy routingStrategy;
    private final OutboundPayloadSerializer payloadSerializer;

    public KafkaOutboundMessagePublisher(KafkaTemplate<String, String> kafkaTemplate,
                                          OutboundRoutingStrategy routingStrategy,
                                          OutboundPayloadSerializer payloadSerializer) {
        this.kafkaTemplate = Require.notNull(kafkaTemplate, "kafkaTemplate");
        this.routingStrategy = Require.notNull(routingStrategy, "routingStrategy");
        this.payloadSerializer = Require.notNull(payloadSerializer, "payloadSerializer");
    }

    @Override
    public CompletionStage<Void> publish(final OutboxEvent message) {

        Require.notNull(message, "message");

        final String destination = routingStrategy.resolveDestination(message);
        final String payload = payloadSerializer.serialize(message);
        final ProducerRecord<String, String> record =
                new ProducerRecord<>(destination, message.aggregateId(), payload);

        addHeaders(message, record);

        log.info("Publishing outbound message eventId={} eventType={} nature={} destination={}",
                message.eventId(), message.eventType(), message.messageNature(), destination);

        return kafkaTemplate.send(record).thenApply(sendResult -> null);
    }

    private static void addHeaders(final OutboxEvent message,
                                   final ProducerRecord<String, String> record) {

        addHeader(record, KafkaHeaders.MESSAGE_ID, message.eventId().toString());
        addHeader(record, KafkaHeaders.EVENT_TYPE, message.eventType());
        addHeader(record, KafkaHeaders.MESSAGE_NATURE, message.messageNature().name());
        addHeader(record, KafkaHeaders.AGGREGATE_ID, message.aggregateId());
        addHeader(record, KafkaHeaders.AGGREGATE_TYPE, message.aggregateType());
        addHeader(record, KafkaHeaders.PAYLOAD_FORMAT, message.payloadFormat());
        addHeaderIfPresent(record, KafkaHeaders.CORRELATION_ID, message.correlationId());
        addHeaderIfPresent(record, KafkaHeaders.CAUSATION_ID, message.causationId());
    }

    private static void addHeader(final ProducerRecord<String, String> record,
                                  final String name,
                                  final String value) {

        record.headers().add(name, value.getBytes(StandardCharsets.UTF_8));
    }

    private static void addHeaderIfPresent(final ProducerRecord<String, String> record,
                                           final String name,
                                           final String value) {
        if (value != null) {
            record.headers().add(name, value.getBytes(StandardCharsets.UTF_8));
        }
    }
}
