package com.arbitrier.order.adapter.outbound.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity for {@link com.arbitrier.order.domain.model.Order}.
 *
 * <p>Kept inside the persistence adapter package. Never returned through application ports.
 *
 * <p>Layer: adapter/outbound/persistence
 * <p>Module: order-service
 */
@Entity
@Table(name = "orders", schema = "order_service")
public class OrderEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "submitted_by", nullable = false)
    private String submittedBy;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.EAGER)
    private List<OrderLineEntity> lines = new ArrayList<>();

    /** Required by JPA. */
    protected OrderEntity() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public List<OrderLineEntity> getLines() { return lines; }
    public void setLines(List<OrderLineEntity> lines) { this.lines = lines; }
}
