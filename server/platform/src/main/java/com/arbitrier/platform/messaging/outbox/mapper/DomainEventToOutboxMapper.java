package com.arbitrier.platform.messaging.outbox.mapper;

import com.arbitrier.platform.messaging.outbox.MessageNature;
import com.arbitrier.platform.messaging.outbox.OutboxEvent;
import com.arbitrier.platform.messaging.outbox.PublishStatus;
import com.arbitrier.platform.messaging.serialization.EventSerializer;
import com.arbitrier.platform.time.TimeProvider;
import com.arbitrier.platform.validation.Require;

import java.util.UUID;

/**
 * Maps outbound message objects (domain events or commands) into {@link OutboxEvent} records
 * ready to be persisted by the transactional outbox.
 *
 * <p>Generates a fresh event identifier, records the current instant as {@code occurredAt},
 * derives the message name from the runtime class simple name, and serializes the payload
 * via the injected {@link EventSerializer}.
 *
 * <p>The three-argument {@link #map(Object, String, String)} overload always produces an
 * {@link MessageNature#EVENT} record and is the default path for domain events. Use the
 * four-argument {@link #map(Object, String, String, MessageNature)} overload when mapping
 * commands or any non-event outbound message.
 *
 * <p>Layer: platform/messaging/outbox/mapper
 * <p>Module: platform
 */
public final class DomainEventToOutboxMapper {

    private static final String JSON_FORMAT = "JSON";

    private final EventSerializer serializer;
    private final TimeProvider timeProvider;

    public DomainEventToOutboxMapper(final EventSerializer serializer, final TimeProvider timeProvider) {

        this.serializer = Require.notNull(serializer, "serializer");
        this.timeProvider = Require.notNull(timeProvider, "timeProvider");
    }

    /**
     * Map the given domain event to an {@link OutboxEvent} in {@link PublishStatus#PENDING} state
     * with {@link MessageNature#EVENT}.
     *
     * @param domainEvent    the source domain event; must not be null
     * @param aggregateId    identifier of the aggregate that produced the event; must not be blank
     * @param aggregateType  short aggregate type name (e.g. {@code "Order"}); must not be blank
     * @return the corresponding {@link OutboxEvent}
     */
    public OutboxEvent map(final Object domainEvent,
                           final String aggregateId,
                           final String aggregateType) {

        return map(domainEvent, aggregateId, aggregateType, MessageNature.EVENT);
    }

    /**
     * Map the given outbound message to an {@link OutboxEvent} in {@link PublishStatus#PENDING}
     * state with the specified {@link MessageNature}.
     *
     * <p>Use this overload when mapping commands or any message that is not a domain event.
     *
     * @param message        the outbound message to map; must not be null
     * @param aggregateId    identifier of the aggregate that produced the message; must not be blank
     * @param aggregateType  short aggregate type name (e.g. {@code "Order"}); must not be blank
     * @param nature         the message nature; must not be null
     * @return the corresponding {@link OutboxEvent}
     */
    public OutboxEvent map(final Object message,
                           final String aggregateId,
                           final String aggregateType,
                           final MessageNature nature) {

        Require.notNull(message, "message");
        Require.notBlank(aggregateId, "aggregateId");
        Require.notBlank(aggregateType, "aggregateType");
        Require.notNull(nature, "nature");

        return new OutboxEvent(
                UUID.randomUUID(),
                aggregateId,
                aggregateType,
                message.getClass().getSimpleName(),
                serializer.serialize(message),
                JSON_FORMAT,
                timeProvider.now(),
                null,
                PublishStatus.PENDING,
                0,
                null,
                null,
                null,
                nature);
    }
}
