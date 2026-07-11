package com.arbitrier.inventory.adapter.outbound.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity for {@link com.arbitrier.inventory.domain.model.StockReservationLine}.
 *
 * <p>Layer: adapter/outbound/persistence
 * <p>Module: inventory-service
 */
@Entity
@Table(name = "stock_reservation_lines", schema = "inventory_service")
public class StockReservationLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private StockReservationEntity reservation;

    @Column(name = "sku_code", nullable = false)
    private String skuCode;

    @Column(name = "requested_quantity", nullable = false)
    private int requestedQuantity;

    @OneToMany(mappedBy = "line", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.EAGER)
    private List<StockAllocationEntity> allocations = new ArrayList<>();

    protected StockReservationLineEntity() {
    }

    public Long getId() { return id; }

    public StockReservationEntity getReservation() { return reservation; }
    public void setReservation(StockReservationEntity reservation) {
        this.reservation = reservation;
    }

    public String getSkuCode() { return skuCode; }
    public void setSkuCode(String skuCode) { this.skuCode = skuCode; }

    public int getRequestedQuantity() { return requestedQuantity; }
    public void setRequestedQuantity(int requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }

    public List<StockAllocationEntity> getAllocations() { return allocations; }
    public void setAllocations(List<StockAllocationEntity> allocations) {
        this.allocations = allocations;
    }
}
