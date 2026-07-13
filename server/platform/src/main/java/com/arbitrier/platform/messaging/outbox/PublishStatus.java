package com.arbitrier.platform.messaging.outbox;

/**
 * Publication lifecycle status of an outbox event.
 *
 * <p>Layer: platform/messaging/outbox
 * <p>Module: platform
 */
public enum PublishStatus {
    /** Event has been persisted but not yet published to the message bus. */
    PENDING,
    /** Event has been successfully published. */
    PUBLISHED,
    /** Publication failed after retries; requires manual intervention. */
    FAILED
}
