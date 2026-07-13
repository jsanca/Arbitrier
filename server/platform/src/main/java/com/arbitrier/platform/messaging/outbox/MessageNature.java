package com.arbitrier.platform.messaging.outbox;

/**
 * Discriminator that identifies the nature of an outbound message stored in the outbox.
 *
 * <p>"Nature" was chosen over "type" to avoid collision with {@code eventType} (the message
 * name) and over "kind" because nature conveys the structural role of the message in the
 * protocol — whether it announces a fact or issues an instruction — not merely a label.
 *
 * <ul>
 *   <li>{@link #EVENT} — a domain event: describes something that has already happened.
 *       Recipients may react but are not obligated to.</li>
 *   <li>{@link #COMMAND} — a command: a request for a specific action to be taken by one
 *       identified recipient.</li>
 * </ul>
 *
 * <p>Layer: platform/messaging/outbox
 * <p>Module: platform
 */
public enum MessageNature {
    /** A domain event — a record of something that happened. */
    EVENT,
    /** A command — an instruction directed at a specific recipient. */
    COMMAND
}
