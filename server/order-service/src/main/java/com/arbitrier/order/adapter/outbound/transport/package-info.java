/**
 * Outbox relay transport layer for the Order domain.
 *
 * <p>This package and its sub-packages implement the transport boundary established in ARB-024.1:
 *
 * <pre>
 * Application Service
 *   ↓
 * OutboxRepository  (transactional write)
 *   ↓ commit
 * Outbox Relay  (reads committed OutboxEvent records)
 *   ↓
 * Transport Layer  ← this package
 *   ├── avro/   Avro encoding + OrderCreated contract mapping (production)
 *   ├── json/   JSON passthrough encoding (development / testing)
 *   ├── kafka/  Kafka producer adapter
 *   └── model/  Shared transport abstractions (TransportEventMetadata)
 *   ↓
 * Kafka
 * </pre>
 *
 * <p><b>Boundary rule:</b> application services must not reference any class in this package tree.
 * Application services write to {@code OutboxRepository}; the relay reads and dispatches.
 *
 * <p><b>Adapter strategy:</b> JSON and Avro are alternative adapters behind the same relay
 * boundary — never parallel publications of the same event.
 *
 * <p>Layer: adapter/outbound/transport
 * <p>Module: order-service
 */
package com.arbitrier.order.adapter.outbound.transport;
