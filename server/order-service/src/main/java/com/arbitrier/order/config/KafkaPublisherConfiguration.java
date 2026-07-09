package com.arbitrier.order.config;

import com.arbitrier.order.adapter.outbound.kafka.KafkaOrderEventPublisher;
import com.arbitrier.order.adapter.outbound.kafka.OrderCreatedAvroMapper;
import com.arbitrier.order.application.port.outbound.OrderEventPublisher;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka producer configuration for the order-service.
 *
 * <p>Activated only when {@code spring.kafka.bootstrap-servers} is set.
 * In tests, this property is absent, so no Kafka beans are created;
 * {@code OrderServiceTestConfiguration} provides a recording
 * {@link com.arbitrier.order.adapter.outbound.RecordingOrderEventPublisher} instead.
 *
 * <h2>Serializer note (ARB-011)</h2>
 * <p>The value serializer defaults to {@link ByteArraySerializer}. At this phase, Avro records
 * are serialized to bytes via {@code GenericData.get().toString()} before publishing, which
 * is a temporary approximation. When Schema Registry is wired, the value serializer must be
 * replaced with {@code io.confluent.kafka.serializers.KafkaAvroSerializer} and the
 * {@code schema.registry.url} producer property set accordingly.
 *
 * <p>OPEN QUESTION (ARB-011): Choose and wire the Avro serializer — either Confluent
 * {@code KafkaAvroSerializer} (requires Schema Registry) or a standalone
 * {@code SpecificAvroSerializer} that embeds the schema in each message.
 *
 * <p>Layer: config
 * <p>Module: order-service
 */
@Configuration
@ConditionalOnProperty("spring.kafka.bootstrap-servers")
public class KafkaPublisherConfiguration {

    @Bean
    ProducerFactory<String, Object> orderProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    KafkaTemplate<String, Object> orderKafkaTemplate(ProducerFactory<String, Object> orderProducerFactory) {
        return new KafkaTemplate<>(orderProducerFactory);
    }

    @Bean
    OrderEventPublisher kafkaOrderEventPublisher(
            KafkaTemplate<String, Object> orderKafkaTemplate,
            OrderCreatedAvroMapper mapper,
            @Value("${arbitrier.kafka.topics.order-created:" +
                    com.arbitrier.platform.kafka.TopicNames.ORDER_CREATED_V1 + "}") String topic) {
        return new KafkaOrderEventPublisher(orderKafkaTemplate, mapper, topic);
    }
}
