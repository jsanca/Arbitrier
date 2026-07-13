package com.arbitrier.platform.spring;

import com.arbitrier.platform.messaging.kafka.outbound.KafkaOutboundMessagePublisher;
import com.arbitrier.platform.messaging.outbox.OutboundMessagePublisher;
import com.arbitrier.platform.messaging.outbox.OutboundPayloadSerializer;
import com.arbitrier.platform.messaging.outbox.OutboundRoutingStrategy;
import com.arbitrier.platform.messaging.serialization.JsonOutboundPayloadSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Auto-configuration for the Kafka-backed {@link OutboundMessagePublisher}.
 *
 * <p>Activates only when:
 * <ol>
 *   <li>{@link KafkaTemplate} is on the classpath ({@code spring-kafka} present), and</li>
 *   <li>{@code spring.kafka.bootstrap-servers} is configured.</li>
 * </ol>
 *
 * <p>Registers a dedicated {@code KafkaTemplate<String, String>} named
 * {@code outboundMessageKafkaTemplate} to avoid conflicts with service-level Avro templates.
 * Also registers a default {@link OutboundPayloadSerializer} backed by
 * {@link JsonOutboundPayloadSerializer}; services may override it by providing their own
 * {@code OutboundPayloadSerializer} bean. The publisher bean additionally requires an
 * {@link OutboundRoutingStrategy} bean; if none is registered the publisher is not created.
 *
 * <p>Layer: platform/spring
 * <p>Module: platform
 */
@AutoConfiguration
@ConditionalOnClass(KafkaTemplate.class)
@ConditionalOnProperty("spring.kafka.bootstrap-servers")
public class KafkaPublisherAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "outboundMessageProducerFactory")
    ProducerFactory<String, String> outboundMessageProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    @ConditionalOnMissingBean(name = "outboundMessageKafkaTemplate")
    KafkaTemplate<String, String> outboundMessageKafkaTemplate(
            ProducerFactory<String, String> outboundMessageProducerFactory) {
        return new KafkaTemplate<>(outboundMessageProducerFactory);
    }

    @Bean
    @ConditionalOnMissingBean(OutboundPayloadSerializer.class)
    OutboundPayloadSerializer jsonOutboundPayloadSerializer() {
        return new JsonOutboundPayloadSerializer();
    }

    @Bean
    @ConditionalOnMissingBean(OutboundMessagePublisher.class)
    @ConditionalOnBean(OutboundRoutingStrategy.class)
    OutboundMessagePublisher kafkaOutboundMessagePublisher(
            KafkaTemplate<String, String> outboundMessageKafkaTemplate,
            OutboundRoutingStrategy outboundRoutingStrategy,
            OutboundPayloadSerializer outboundPayloadSerializer) {
        return new KafkaOutboundMessagePublisher(
                outboundMessageKafkaTemplate, outboundRoutingStrategy, outboundPayloadSerializer);
    }
}
