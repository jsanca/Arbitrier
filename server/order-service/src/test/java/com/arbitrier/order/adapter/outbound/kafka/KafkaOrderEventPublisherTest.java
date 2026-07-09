package com.arbitrier.order.adapter.outbound.kafka;

import com.arbitrier.contracts.common.MessageMetadata;
import com.arbitrier.contracts.common.MoneyAmount;
import com.arbitrier.contracts.order.OrderCreated;
import com.arbitrier.order.domain.event.OrderCreatedDomainEvent;
import com.arbitrier.order.domain.model.CustomerId;
import com.arbitrier.order.domain.model.OrderId;
import com.arbitrier.order.domain.model.OrderLine;
import com.arbitrier.order.domain.model.Quantity;
import com.arbitrier.order.domain.model.Sku;
import com.arbitrier.order.domain.model.UserId;
import com.arbitrier.platform.kafka.KafkaHeaders;
import com.arbitrier.platform.kafka.TopicNames;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link KafkaOrderEventPublisher}.
 *
 * <p>Uses mocked {@link KafkaTemplate} and {@link OrderCreatedAvroMapper} — no Kafka broker required.
 */
@ExtendWith(MockitoExtension.class)
class KafkaOrderEventPublisherTest {

    @Mock
    KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    OrderCreatedAvroMapper mapper;

    private KafkaOrderEventPublisher publisher;

    private static final String TOPIC = TopicNames.ORDER_CREATED_V1;
    private static final String ORDER_ID = "order-pub-test-001";
    private static final String CORRELATION_ID = "test-corr-id";
    private static final String MESSAGE_ID = "msg-uuid-001";

    @BeforeEach
    void setUp() {
        publisher = new KafkaOrderEventPublisher(kafkaTemplate, mapper, TOPIC);
    }

    @Test
    void publishes_to_expected_topic() {
        OrderCreatedDomainEvent event = buildEvent();
        when(mapper.map(any(), any())).thenReturn(buildAvro(MESSAGE_ID, CORRELATION_ID));

        publisher.publish(event);

        ArgumentCaptor<ProducerRecord<String, Object>> captor = producerRecordCaptor();
        verify(kafkaTemplate).send(captor.capture());
        assertThat(captor.getValue().topic()).isEqualTo(TOPIC);
    }

    @Test
    void uses_orderId_as_partition_key() {
        OrderCreatedDomainEvent event = buildEvent();
        when(mapper.map(any(), any())).thenReturn(buildAvro(MESSAGE_ID, CORRELATION_ID));

        publisher.publish(event);

        ArgumentCaptor<ProducerRecord<String, Object>> captor = producerRecordCaptor();
        verify(kafkaTemplate).send(captor.capture());
        assertThat(captor.getValue().key()).isEqualTo(ORDER_ID);
    }

    @Test
    void sends_avro_OrderCreated_as_value() {
        OrderCreatedDomainEvent event = buildEvent();
        OrderCreated avro = buildAvro(MESSAGE_ID, CORRELATION_ID);
        when(mapper.map(any(), any())).thenReturn(avro);

        publisher.publish(event);

        ArgumentCaptor<ProducerRecord<String, Object>> captor = producerRecordCaptor();
        verify(kafkaTemplate).send(captor.capture());
        assertThat(captor.getValue().value()).isInstanceOf(OrderCreated.class);
        assertThat(captor.getValue().value()).isSameAs(avro);
    }

    @Test
    void attaches_message_id_header() {
        OrderCreatedDomainEvent event = buildEvent();
        when(mapper.map(any(), any())).thenReturn(buildAvro(MESSAGE_ID, CORRELATION_ID));

        publisher.publish(event);

        ArgumentCaptor<ProducerRecord<String, Object>> captor = producerRecordCaptor();
        verify(kafkaTemplate).send(captor.capture());
        assertThat(headerValue(captor.getValue(), KafkaHeaders.MESSAGE_ID)).isEqualTo(MESSAGE_ID);
    }

    @Test
    void attaches_correlation_id_header_from_mdc() {
        MDC.put(com.arbitrier.platform.logging.StructuredLogFields.CORRELATION_ID, CORRELATION_ID);
        try {
            OrderCreatedDomainEvent event = buildEvent();
            when(mapper.map(any(), any())).thenReturn(buildAvro(MESSAGE_ID, CORRELATION_ID));

            publisher.publish(event);

            ArgumentCaptor<ProducerRecord<String, Object>> captor = producerRecordCaptor();
            verify(kafkaTemplate).send(captor.capture());
            assertThat(headerValue(captor.getValue(), KafkaHeaders.CORRELATION_ID))
                    .isEqualTo(CORRELATION_ID);
        } finally {
            MDC.remove(com.arbitrier.platform.logging.StructuredLogFields.CORRELATION_ID);
        }
    }

    @Test
    void generates_correlation_id_header_when_mdc_absent() {
        MDC.remove(com.arbitrier.platform.logging.StructuredLogFields.CORRELATION_ID);
        OrderCreatedDomainEvent event = buildEvent();
        when(mapper.map(any(), any())).thenReturn(buildAvro(MESSAGE_ID, "generated-uuid"));

        publisher.publish(event);

        ArgumentCaptor<ProducerRecord<String, Object>> captor = producerRecordCaptor();
        verify(kafkaTemplate).send(captor.capture());
        assertThat(headerValue(captor.getValue(), KafkaHeaders.CORRELATION_ID)).isNotBlank();
    }

    @Test
    void attaches_schema_version_header() {
        OrderCreatedDomainEvent event = buildEvent();
        when(mapper.map(any(), any())).thenReturn(buildAvro(MESSAGE_ID, CORRELATION_ID));

        publisher.publish(event);

        ArgumentCaptor<ProducerRecord<String, Object>> captor = producerRecordCaptor();
        verify(kafkaTemplate).send(captor.capture());
        assertThat(headerValue(captor.getValue(), KafkaHeaders.SCHEMA_VERSION)).isEqualTo("v1");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<ProducerRecord<String, Object>> producerRecordCaptor() {
        return ArgumentCaptor.forClass((Class<ProducerRecord<String, Object>>) (Class<?>) ProducerRecord.class);
    }

    private String headerValue(ProducerRecord<String, Object> record, String headerName) {
        Header header = record.headers().lastHeader(headerName);
        assertThat(header).as("header '%s'", headerName).isNotNull();
        return new String(header.value(), StandardCharsets.UTF_8);
    }

    private OrderCreatedDomainEvent buildEvent() {
        return new OrderCreatedDomainEvent(
                new OrderId(ORDER_ID),
                new CustomerId("cust-001"),
                new UserId("user-001"),
                List.of(new OrderLine(new Sku("SKU-A"), new Quantity(5))));
    }

    private OrderCreated buildAvro(String messageId, String correlationId) {
        MessageMetadata metadata = MessageMetadata.newBuilder()
                .setMessageId(messageId)
                .setCorrelationId(correlationId)
                .setCausationId(null)
                .setOccurredAt("2026-07-08T10:00:00Z")
                .setSchemaVersion("v1")
                .build();
        return OrderCreated.newBuilder()
                .setMetadata(metadata)
                .setOrderId(ORDER_ID)
                .setCustomerId("cust-001")
                .setSubmittedByUserId("user-001")
                .setLines(List.of())
                .setRequestedTotal(MoneyAmount.newBuilder()
                        .setAmount("0")
                        .setCurrency("USD")
                        .build())
                .build();
    }
}
