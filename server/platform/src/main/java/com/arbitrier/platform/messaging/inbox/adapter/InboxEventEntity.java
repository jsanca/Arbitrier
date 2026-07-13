package com.arbitrier.platform.messaging.inbox.adapter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for inbox event rows.
 *
 * <p>The schema is resolved by {@code hibernate.default_schema} set in each service's
 * {@code application.yml}.
 *
 * <p>Layer: adapter/outbound/persistence
 * <p>Module: platform
 */
@Entity
@Table(name = "inbox_events")
public class InboxEventEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "consumer_id", nullable = false)
    private String consumerId;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "processing_status", nullable = false)
    private String processingStatus;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "payload_hash")
    private String payloadHash;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /** Required by JPA. */
    protected InboxEventEntity() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getConsumerId() { return consumerId; }
    public void setConsumerId(String consumerId) { this.consumerId = consumerId; }

    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }

    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }

    public String getProcessingStatus() { return processingStatus; }
    public void setProcessingStatus(String processingStatus) { this.processingStatus = processingStatus; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getPayloadHash() { return payloadHash; }
    public void setPayloadHash(String payloadHash) { this.payloadHash = payloadHash; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
