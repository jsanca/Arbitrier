package com.arbitrier.credit.adapter.outbound.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;

/**
 * JPA entity for {@link com.arbitrier.credit.domain.model.CreditReservation}.
 *
 * <p>Layer: adapter/outbound/persistence
 * <p>Module: credit-service
 */
@Entity
@Table(name = "credit_reservations", schema = "credit_service")
public class CreditReservationEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "amount_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal amountValue;

    @Column(name = "amount_currency", nullable = false, length = 3)
    private String amountCurrency;

    @Column(name = "status", nullable = false)
    private String status;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected CreditReservationEntity() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public BigDecimal getAmountValue() { return amountValue; }
    public void setAmountValue(BigDecimal amountValue) { this.amountValue = amountValue; }

    public String getAmountCurrency() { return amountCurrency; }
    public void setAmountCurrency(String amountCurrency) { this.amountCurrency = amountCurrency; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
