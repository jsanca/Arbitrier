package com.arbitrier.inventory.adapter.outbound.persistence;

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
 * JPA entity for {@link com.arbitrier.inventory.domain.model.StockReservation}.
 *
 * <p>Layer: adapter/outbound/persistence
 * <p>Module: inventory-service
 */
@Entity
@Table(name = "stock_reservations", schema = "inventory_service")
public class StockReservationEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "status", nullable = false)
    private String status;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.EAGER)
    private List<StockReservationLineEntity> lines = new ArrayList<>();

    protected StockReservationEntity() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public List<StockReservationLineEntity> getLines() { return lines; }
    public void setLines(List<StockReservationLineEntity> lines) { this.lines = lines; }
}
