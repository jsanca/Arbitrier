package com.arbitrier.orchestrator.adapter.outbound.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * JPA entity for {@link com.arbitrier.orchestrator.domain.model.Saga}.
 *
 * <p>All saga state is persisted in explicit columns — no serialized blobs.
 *
 * <p>Layer: adapter/outbound/persistence
 * <p>Module: orchestrator-service
 */
@Entity
@Table(name = "sagas", schema = "orchestrator_service")
public class SagaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "current_step", nullable = false)
    private String currentStep;

    @Column(name = "customer_decision")
    private String customerDecision;

    @Column(name = "stock_reservation_id")
    private String stockReservationId;

    @Column(name = "credit_reservation_id")
    private String creditReservationId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected SagaEntity() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCurrentStep() { return currentStep; }
    public void setCurrentStep(String currentStep) { this.currentStep = currentStep; }

    public String getCustomerDecision() { return customerDecision; }
    public void setCustomerDecision(String customerDecision) {
        this.customerDecision = customerDecision;
    }

    public String getStockReservationId() { return stockReservationId; }
    public void setStockReservationId(String stockReservationId) {
        this.stockReservationId = stockReservationId;
    }

    public String getCreditReservationId() { return creditReservationId; }
    public void setCreditReservationId(String creditReservationId) {
        this.creditReservationId = creditReservationId;
    }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
