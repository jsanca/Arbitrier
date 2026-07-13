package com.arbitrier.platform.messaging.outbox.adapter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for outbox event rows.
 *
 * <p>The schema is resolved by {@code hibernate.default_schema} set in each service's
 * {@code application.yml}. This entity intentionally carries no schema qualifier so that
 * the same class is reused across all services.
 *
 * <p>Layer: adapter/outbound/persistence
 * <p>Module: platform
 */
@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "payload_format", nullable = false)
    private String payloadFormat;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "publish_status", nullable = false)
    private String publishStatus;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_attempt")
    private Instant lastAttempt;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "causation_id")
    private String causationId;

    @Column(name = "message_nature", nullable = false)
    private String messageNature;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /** Required by JPA. */
    protected OutboxEventEntity() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getAggregateId() { return aggregateId; }
    public void setAggregateId(String aggregateId) { this.aggregateId = aggregateId; }

    public String getAggregateType() { return aggregateType; }
    public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public String getPayloadFormat() { return payloadFormat; }
    public void setPayloadFormat(String payloadFormat) { this.payloadFormat = payloadFormat; }

    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }

    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }

    public String getPublishStatus() { return publishStatus; }
    public void setPublishStatus(String publishStatus) { this.publishStatus = publishStatus; }

    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }

    public Instant getLastAttempt() { return lastAttempt; }
    public void setLastAttempt(Instant lastAttempt) { this.lastAttempt = lastAttempt; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getCausationId() { return causationId; }
    public void setCausationId(String causationId) { this.causationId = causationId; }

    public String getMessageNature() { return messageNature; }
    public void setMessageNature(String messageNature) { this.messageNature = messageNature; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
