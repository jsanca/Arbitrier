/**
 * Outbox relay transport adapters for the Order domain.
 *
 * <p><b>Publication boundary (ARB-024.1):</b> classes in this package belong to the
 * Outbox relay transport layer, not to any application service. Application services
 * must not reference these classes directly; they publish domain events exclusively
 * through {@link com.arbitrier.platform.messaging.outbox.OutboxRepository}.
 *
 * <p>The Outbox relay reads committed {@code OutboxEvent} records from the database and
 * dispatches them to Kafka via a transport adapter (JSON for development, Avro + Schema
 * Registry for production). These are alternative adapters behind the same transport
 * boundary — never parallel publications of the same event.
 *
 * <p>Current contents:
 * <ul>
 *   <li>{@link com.arbitrier.order.adapter.outbound.kafka.OrderCreatedAvroMapper} —
 *       maps {@code OrderCreatedDomainEvent} to the {@code OrderCreated} Avro contract;
 *       wired by the future Avro transport configuration (ARB-024.x).</li>
 * </ul>
 *
 * <p>Layer: adapter/outbound/kafka
 * <p>Module: order-service
 */
package com.arbitrier.order.adapter.outbound.kafka;
