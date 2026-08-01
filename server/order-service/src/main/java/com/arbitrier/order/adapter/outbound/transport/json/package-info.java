/**
 * JSON transport adapter for the Order domain (development / test encoding).
 *
 * <p>Publishes {@code OutboxEvent.payload()} directly to Kafka as UTF-8 JSON without Avro
 * encoding or Schema Registry. This adapter is activated by configuration in non-production
 * environments where Schema Registry is not available.
 *
 * <p>The JSON payload is already produced by {@code JacksonEventSerializer} when the Outbox
 * record is written, so this adapter requires no additional serialization step.
 *
 * <p>Layer: adapter/outbound/transport/json
 * <p>Module: order-service
 */
package com.arbitrier.order.adapter.outbound.transport.json;
