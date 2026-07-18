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
 * <h2>Lifecycle</h2>
 * <pre>
 *   PENDING → claim()        → CLAIMED
 *   CLAIMED → markPublished() → PUBLISHED  (claim metadata cleared)
 *   CLAIMED → markFailed()    → FAILED     (claim metadata cleared)
 * </pre>
 *
 * <h2>Claim invariants</h2>
 * <ul>
 *   <li>PENDING — {@code claimedBy == null}, {@code claimedAt == null}</li>
 *   <li>CLAIMED — {@code claimedBy} non-blank, {@code claimedAt} non-null</li>
 *   <li>PUBLISHED — {@code claimedBy == null}, {@code claimedAt == null}</li>
 *   <li>FAILED — {@code claimedBy == null}, {@code claimedAt == null}</li>
 * </ul>
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
 * @param publishedAt     instant at which the message was published, or {@code null}
 * @param publishStatus   current lifecycle status
 * @param attemptCount    number of publication attempts made so far
 * @param lastAttempt     instant of the most recent publication attempt, or {@code null}
 * @param correlationId   business correlation identifier, or {@code null}
 * @param causationId     identifier of the message that caused this message, or {@code null}
 * @param messageNature   whether this message is an {@link MessageNature#EVENT} or a {@link MessageNature#COMMAND}
 * @param claimedBy       worker identifier that currently owns this event, or {@code null} if unclaimed
 * @param claimedAt       instant at which the event was claimed, or {@code null} if unclaimed
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
        MessageNature messageNature,
        String claimedBy,
        Instant claimedAt) {

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

        if (publishStatus == PublishStatus.CLAIMED) {
            if (claimedBy == null || claimedBy.isBlank()) {
                throw new IllegalArgumentException("CLAIMED event must have a non-blank claimedBy");
            }
            if (claimedAt == null) {
                throw new NullPointerException("CLAIMED event must have a non-null claimedAt");
            }
        } else {
            if (claimedBy != null) {
                throw new IllegalArgumentException(
                        "Non-CLAIMED event must not have claimedBy, status=" + publishStatus);
            }
            if (claimedAt != null) {
                throw new IllegalArgumentException(
                        "Non-CLAIMED event must not have claimedAt, status=" + publishStatus);
            }
        }
    }

    /**
     * Transition this PENDING event to CLAIMED, recording the claiming worker.
     *
     * @param workerId  non-blank identifier of the claiming worker
     * @param claimedAt non-null instant at which the claim was made
     * @return a new {@link OutboxEvent} in {@link PublishStatus#CLAIMED} state
     * @throws IllegalStateException    if this event is not in PENDING status
     * @throws IllegalArgumentException if {@code workerId} is blank
     * @throws NullPointerException     if {@code claimedAt} is null
     */
    public OutboxEvent claim(final String workerId, final Instant claimedAt) {
        if (publishStatus != PublishStatus.PENDING) {
            throw new IllegalStateException(
                    "Only PENDING events can be claimed, current status: " + publishStatus);
        }
        Require.notBlank(workerId, "workerId");
        Require.notNull(claimedAt, "claimedAt");
        return new OutboxEvent(
                eventId, aggregateId, aggregateType, eventType, payload, payloadFormat,
                occurredAt, publishedAt, PublishStatus.CLAIMED, attemptCount, lastAttempt,
                correlationId, causationId, messageNature,
                workerId, claimedAt);
    }

    /**
     * Transition this CLAIMED event to PUBLISHED, clearing the claim metadata.
     *
     * @param publishedAt non-null instant at which publication was confirmed
     * @return a new {@link OutboxEvent} in {@link PublishStatus#PUBLISHED} state with no claim metadata
     * @throws IllegalStateException if this event is not in CLAIMED status
     * @throws NullPointerException  if {@code publishedAt} is null
     */
    public OutboxEvent markPublished(final Instant publishedAt) {
        if (publishStatus != PublishStatus.CLAIMED) {
            throw new IllegalStateException(
                    "Only CLAIMED events can be marked published, current status: " + publishStatus);
        }
        Require.notNull(publishedAt, "publishedAt");
        return new OutboxEvent(
                eventId, aggregateId, aggregateType, eventType, payload, payloadFormat,
                occurredAt, publishedAt, PublishStatus.PUBLISHED, attemptCount, lastAttempt,
                correlationId, causationId, messageNature,
                null, null);
    }

    /**
     * Transition this CLAIMED event to FAILED, clearing the claim metadata and incrementing
     * the attempt count.
     *
     * @param failedAt non-null instant at which the failure was recorded
     * @return a new {@link OutboxEvent} in {@link PublishStatus#FAILED} state with no claim metadata
     * @throws IllegalStateException if this event is not in CLAIMED status
     * @throws NullPointerException  if {@code failedAt} is null
     */
    public OutboxEvent markFailed(final Instant failedAt) {
        if (publishStatus != PublishStatus.CLAIMED) {
            throw new IllegalStateException(
                    "Only CLAIMED events can be marked failed, current status: " + publishStatus);
        }
        Require.notNull(failedAt, "failedAt");
        return new OutboxEvent(
                eventId, aggregateId, aggregateType, eventType, payload, payloadFormat,
                occurredAt, publishedAt, PublishStatus.FAILED, attemptCount + 1, failedAt,
                correlationId, causationId, messageNature,
                null, null);
    }
}
