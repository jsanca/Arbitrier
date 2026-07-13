package com.arbitrier.platform.messaging.serialization;

import com.arbitrier.platform.messaging.outbox.OutboxEvent;
import com.arbitrier.platform.messaging.outbox.OutboundPayloadSerializer;
import com.arbitrier.platform.validation.Require;

/**
 * Development {@link OutboundPayloadSerializer} that forwards the pre-serialized
 * {@link OutboxEvent#payload()} unchanged.
 *
 * <p>The outbox pipeline already stores the event as a JSON string via
 * {@link EventSerializer}. This implementation simply surfaces that stored payload
 * for transmission — no re-serialization or format conversion occurs.
 *
 * <p>A future Avro implementation will re-encode the payload from the stored JSON
 * into an Avro binary or Avro-JSON wire format without changing this interface.
 *
 * <p>Layer: platform/messaging/serialization
 * <p>Module: platform
 */
public final class JsonOutboundPayloadSerializer implements OutboundPayloadSerializer {

    @Override
    public String serialize(final OutboxEvent event) {
        Require.notNull(event, "event");
        return event.payload();
    }
}
