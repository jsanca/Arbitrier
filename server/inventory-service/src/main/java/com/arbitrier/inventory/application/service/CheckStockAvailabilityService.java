package com.arbitrier.inventory.application.service;

import com.arbitrier.inventory.application.port.inbound.CheckStockAvailabilityCommand;
import com.arbitrier.inventory.application.port.inbound.CheckStockAvailabilityLineResult;
import com.arbitrier.inventory.application.port.inbound.CheckStockAvailabilityResult;
import com.arbitrier.inventory.application.port.inbound.CheckStockAvailabilityUseCase;
import com.arbitrier.inventory.application.port.outbound.AllocationPlan;
import com.arbitrier.inventory.application.port.outbound.RequestedStockLine;
import com.arbitrier.inventory.application.port.outbound.WarehouseAllocationPort;
import com.arbitrier.platform.validation.Require;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Use-case implementation: checks global stock availability for a set of SKUs without reserving.
 *
 * <h2>Per-line computation</h2>
 * <ul>
 *   <li>{@code availableQuantity} = total allocated across all warehouses (already capped at
 *       the requested quantity by the {@link WarehouseAllocationPort} contract).</li>
 *   <li>{@code backorderQuantity = requestedQuantity - availableQuantity}</li>
 *   <li>{@code fullyAvailable = availableQuantity >= requestedQuantity}</li>
 * </ul>
 *
 * <h2>Advisory nature</h2>
 * <p>This result is non-binding. Available quantities may change between the check and the
 * actual reservation attempt inside the saga. The reservation outcome is authoritative.
 *
 * <h2>No side effects</h2>
 * <p>This service does not save anything to a repository and does not publish any events.
 *
 * <p>Layer: application/service
 * <p>Module: inventory-service
 */
public class CheckStockAvailabilityService implements CheckStockAvailabilityUseCase {

    private static final Logger log = LoggerFactory.getLogger(CheckStockAvailabilityService.class);

    private final WarehouseAllocationPort warehouseAllocationPort;

    public CheckStockAvailabilityService(final WarehouseAllocationPort warehouseAllocationPort) {
        this.warehouseAllocationPort = Require.notNull(warehouseAllocationPort, "warehouseAllocationPort");
    }

    @Override
    public CheckStockAvailabilityResult check(final CheckStockAvailabilityCommand command) {

        Require.notNull(command, "command");

        final List<RequestedStockLine> requestedLines = command.lines().stream()
                .map(l -> new RequestedStockLine(l.sku(), l.requestedQuantity()))
                .toList();
        final AllocationPlan plan = warehouseAllocationPort.allocate(requestedLines);
        final List<CheckStockAvailabilityLineResult> lines = computeLineResults(command, plan);

        log.info("Stock availability check lines={}", lines.size());

        return new CheckStockAvailabilityResult(lines);
    }

    private List<CheckStockAvailabilityLineResult> computeLineResults(
            final CheckStockAvailabilityCommand command, final AllocationPlan plan) {

        return command.lines().stream()
                .map(line -> {
                    final int available = plan.totalAllocated(line.sku());
                    final int backorder = Math.max(0, line.requestedQuantity() - available);
                    final boolean fullyAvailable = available >= line.requestedQuantity();
                    return new CheckStockAvailabilityLineResult(
                            line.sku(), line.requestedQuantity(), available, fullyAvailable, backorder);
                })
                .toList();
    }
}
