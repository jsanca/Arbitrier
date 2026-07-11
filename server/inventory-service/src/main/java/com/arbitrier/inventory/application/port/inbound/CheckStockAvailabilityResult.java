package com.arbitrier.inventory.application.port.inbound;

import java.util.List;

/**
 * Result of a stock availability check: one {@link CheckStockAvailabilityLineResult} per
 * requested SKU.
 *
 * <p>This result is advisory and non-binding. Available quantities may change before an
 * actual reservation is attempted. The reservation outcome inside the saga is authoritative.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: inventory-service
 */
public record CheckStockAvailabilityResult(List<CheckStockAvailabilityLineResult> lines) {

    public CheckStockAvailabilityResult {
        lines = List.copyOf(lines);
    }
}
