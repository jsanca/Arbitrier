package com.arbitrier.platform.messaging.outbox;

import com.arbitrier.platform.validation.Require;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable value carrier for an outbox message row.
 *
 * <p>The outbox pattern stores each outbound message (domain event or command) as a row in
 * the same database transaction as the aggregate change. A separate publisher process later
 * reads pending rows and dispatches them to the appropriate runtime destination.
 *
 * <p>{@link #eventType} is the message name used for routing (e.g.
 * {@code "OrderCreatedDomainEvent"}, {@code "ReserveStockCommand"}). The
 * {@link #messageNature} discriminator distinguishes domain events from commands so that
 * routing strategies and dispatch logic can handle them differently without inspecting the
 * name string.
 *
 * <p>Layer: platform/messaging/outbox
 * <p>Module: platform
 *
 * @param eventId         globally unique message identifier
 * @param aggregateId     identifier of the aggregate that produced the message
 * @param aggregateType   short type name of the aggregate (e.g. {@code "Order"})
 * @param eventType       message name used for routing (e.g. {@code "OrderCreatedDomainEvent"})
 * @param payload         serialized message payload
 * @param payloadFormat   payload format identifier (e.g. {@code "JSON"})
 * @param occurredAt      instant at which the message was produced
 * @param publishedAt     instant at which the message was published, or {@code null} if not yet published
 * @param publishStatus   current lifecycle status
 * @param attemptCount    number of publication attempts made so far
 * @param lastAttempt     instant of the most recent publication attempt, or {@code null}
 * @param correlationId   business correlation identifier propagated through the message, or {@code null}
 * @param causationId     identifier of the message that caused this message, or {@code null}
 * @param messageNature   whether this message is an {@link MessageNature#EVENT} or a {@link MessageNature#COMMAND}
 */
public record OutboxEvent(
        UUID eventId,
        String aggregateId,
        String aggregateType,
        String eventType,
        String payload,
        String payloadFormat,
        Instant occurredAt,
        Instant publishedAt,
        PublishStatus publishStatus,
        int attemptCount,
        Instant lastAttempt,
        String correlationId,
        String causationId,
        MessageNature messageNature) {

    public OutboxEvent {
        Require.notNull(eventId, "eventId");
        Require.notBlank(aggregateId, "aggregateId");
        Require.notBlank(aggregateType, "aggregateType");
        Require.notBlank(eventType, "eventType");
        Require.notBlank(payload, "payload");
        Require.notBlank(payloadFormat, "payloadFormat");
        Require.notNull(occurredAt, "occurredAt");
        Require.notNull(publishStatus, "publishStatus");
        Require.notNull(messageNature, "messageNature");
    }
}
