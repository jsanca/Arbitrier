/**
 * Domain event identity contracts.
 *
 * <p>{@link com.arbitrier.platform.messaging.event.DomainEvent} is implemented by domain events
 * that want to declare a stable logical identity. The Outbox mapper reads
 * {@link com.arbitrier.platform.messaging.event.EventDescriptor} from the event and uses its
 * {@code type} as the outbox {@code eventType} field instead of the Java class simple name.
 *
 * <p>This package has no Spring, Kafka, Avro, or persistence dependencies.
 *
 * <p>Layer: platform/messaging/event
 * <p>Module: platform
 */
package com.arbitrier.platform.messaging.event;
