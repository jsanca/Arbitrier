package com.arbitrier.platform.messaging.inbox;

/**
 * Processing lifecycle status of an inbox event.
 *
 * <p>Layer: platform/messaging/inbox
 * <p>Module: platform
 */
public enum ProcessingStatus {
    /** Event has been received but not yet processed by the consumer. */
    PENDING,
    /** Event has been processed successfully. */
    PROCESSED,
    /** Event processing failed and will not be retried. */
    FAILED
}
