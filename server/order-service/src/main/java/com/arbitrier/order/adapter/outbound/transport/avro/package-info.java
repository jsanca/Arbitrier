/**
 * Avro transport adapter for the Order domain (production encoding).
 *
 * <p>Converts {@code OutboxEvent} records to Avro-encoded Kafka messages using the
 * {@code OrderCreated} Avro contract from {@code arbitrier-contracts}.
 *
 * <p>This is the production transport adapter. The development adapter lives in
 * {@code transport/json}. Both implement the same relay boundary; only one is active at runtime.
 *
 * <p>Schema Registry subject naming will be wired in ARB-024.x once the subject strategy is
 * defined. Inputs: {@link com.arbitrier.order.adapter.outbound.transport.model.TransportEventMetadata}
 * derived from {@code OutboxEvent.eventType} and the event's {@link com.arbitrier.platform.messaging.event.EventDescriptor}.
 *
 * <p>Layer: adapter/outbound/transport/avro
 * <p>Module: order-service
 */
package com.arbitrier.order.adapter.outbound.transport.avro;
