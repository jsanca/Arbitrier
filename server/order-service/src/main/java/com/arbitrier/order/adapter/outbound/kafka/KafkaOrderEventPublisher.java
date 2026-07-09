package com.arbitrier.order.adapter.outbound.kafka;

import com.arbitrier.contracts.order.OrderCreated;
import com.arbitrier.order.application.port.outbound.OrderEventPublisher;
import com.arbitrier.order.domain.event.OrderCreatedDomainEvent;
import com.arbitrier.platform.kafka.KafkaHeaders;
import com.arbitrier.platform.logging.StructuredLogFields;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Kafka outbound adapter: publishes {@link OrderCreatedDomainEvent} as an Avro
 * {@link OrderCreated} record to the configured topic.
 *
 * <h2>Header strategy</h2>
 * <ul>
 *   <li>{@link KafkaHeaders#MESSAGE_ID} — unique ID generated per message by the mapper.</li>
 *   <li>{@link KafkaHeaders#CORRELATION_ID} — read from MDC; falls back to a new UUID if absent.</li>
 *   <li>{@link KafkaHeaders#SCHEMA_VERSION} — always {@code "v1"} for this message type.</li>
 *   <li>{@link KafkaHeaders#TRACEPARENT} / {@link KafkaHeaders#TRACESTATE} — intentionally absent;
 *       W3C Trace Context propagation into Kafka is deferred to the OpenTelemetry
 *       Kafka instrumentation phase (ADR-0008).</li>
 * </ul>
 *
 * <h2>Logging</h2>
 * <p>Only safe business IDs ({@code orderId}, {@code correlationId}) are logged — no PII.
 *
 * <p>Layer: adapter/outbound/kafka
 * <p>Module: order-service
 */
public class KafkaOrderEventPublisher implements OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaOrderEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OrderCreatedAvroMapper mapper;
    private final String topic;

    public KafkaOrderEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            OrderCreatedAvroMapper mapper,
            String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.mapper = mapper;
        this.topic = topic;
    }

    @Override
    public void publish(OrderCreatedDomainEvent event) {
        String correlationId = MDC.get(StructuredLogFields.CORRELATION_ID);
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }

        OrderCreated avro = mapper.map(event, correlationId);

        ProducerRecord<String, Object> record =
                new ProducerRecord<>(topic, event.orderId().value(), avro);

        record.headers().add(
                KafkaHeaders.MESSAGE_ID,
                avro.getMetadata().getMessageId().getBytes(StandardCharsets.UTF_8));
        record.headers().add(
                KafkaHeaders.CORRELATION_ID,
                correlationId.getBytes(StandardCharsets.UTF_8));
        record.headers().add(
                KafkaHeaders.SCHEMA_VERSION,
                avro.getMetadata().getSchemaVersion().getBytes(StandardCharsets.UTF_8));

        log.info("Publishing OrderCreated orderId={} correlationId={}",
                event.orderId().value(), correlationId);

        kafkaTemplate.send(record);
    }
}
