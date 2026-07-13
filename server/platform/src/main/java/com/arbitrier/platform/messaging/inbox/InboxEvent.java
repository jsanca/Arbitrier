package com.arbitrier.platform.messaging.inbox;

import com.arbitrier.platform.validation.Require;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable value carrier for an inbox event row.
 *
 * <p>The inbox pattern records incoming events so that consumers can deduplicate replayed
 * messages (idempotent consumption).
 *
 * <p>Layer: platform/messaging/inbox
 * <p>Module: platform
 *
 * @param eventId          unique event identifier as delivered by the message bus
 * @param consumerId       identifier of the consumer that received the event
 * @param receivedAt       instant at which the event was received
 * @param processedAt      instant at which processing completed, or {@code null} if still pending
 * @param processingStatus current processing lifecycle status
 * @param correlationId    business correlation identifier, or {@code null}
 * @param payloadHash      hash of the payload for tamper detection, or {@code null}
 */
public record InboxEvent(
        UUID eventId,
        String consumerId,
        Instant receivedAt,
        Instant processedAt,
        ProcessingStatus processingStatus,
        String correlationId,
        String payloadHash) {

    public InboxEvent {
        Require.notNull(eventId, "eventId");
        Require.notBlank(consumerId, "consumerId");
        Require.notNull(receivedAt, "receivedAt");
        Require.notNull(processingStatus, "processingStatus");
    }
}
