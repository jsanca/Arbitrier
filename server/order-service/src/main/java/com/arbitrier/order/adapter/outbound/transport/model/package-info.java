/**
 * Shared transport abstractions for the Order domain relay layer.
 *
 * <p>{@link com.arbitrier.order.adapter.outbound.transport.model.TransportEventMetadata} is the
 * primary type here: it bridges the domain's {@link com.arbitrier.platform.messaging.event.EventDescriptor}
 * (logical identity) to the string encodings expected by Kafka headers and Avro metadata fields.
 *
 * <p>This package has no Kafka, Avro, or Schema Registry dependencies — it holds only
 * the shared model that both JSON and Avro adapters consume.
 *
 * <p>Layer: adapter/outbound/transport/model
 * <p>Module: order-service
 */
package com.arbitrier.order.adapter.outbound.transport.model;
