/**
 * Kafka producer adapter for the Order domain.
 *
 * <p>Sends serialized (JSON or Avro) transport payloads to Kafka. This adapter is
 * encoding-agnostic: it receives an already-serialized byte payload and the required
 * Kafka metadata (topic, partition key, headers) from the relay, and delegates only
 * the actual I/O to {@code KafkaTemplate}.
 *
 * <p>Configuration ({@code KafkaTemplate}, topic names, bootstrap-servers) is provided
 * by the relay's Spring configuration when Kafka is available.
 *
 * <p>Layer: adapter/outbound/transport/kafka
 * <p>Module: order-service
 */
package com.arbitrier.order.adapter.outbound.transport.kafka;
