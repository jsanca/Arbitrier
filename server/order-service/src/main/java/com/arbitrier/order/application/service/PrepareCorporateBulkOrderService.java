package com.arbitrier.order.application.service;

import com.arbitrier.order.application.port.inbound.PrepareCorporateBulkOrderCommand;
import com.arbitrier.order.application.port.inbound.PrepareCorporateBulkOrderLineResult;
import com.arbitrier.order.application.port.inbound.PrepareCorporateBulkOrderResult;
import com.arbitrier.order.application.port.inbound.PrepareCorporateBulkOrderUseCase;
import com.arbitrier.order.application.port.inbound.RecommendedAction;
import com.arbitrier.order.application.port.outbound.AvailabilityLineQuery;
import com.arbitrier.order.application.port.outbound.AvailabilityLineResponse;
import com.arbitrier.order.application.port.outbound.InventoryAvailabilityPort;
import com.arbitrier.platform.validation.Require;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Use-case implementation: pre-saga availability negotiation for a corporate bulk order.
 *
 * <h2>Flow</h2>
 * <ol>
 *   <li>Query {@link InventoryAvailabilityPort} for global per-SKU stock levels.</li>
 *   <li>Compute per-line available and backorder quantities.</li>
 *   <li>Determine a {@link RecommendedAction}:
 *     <ul>
 *       <li>{@code PROCEED_FULL} — all lines fully available.</li>
 *       <li>{@code ASK_CUSTOMER_ACCEPT_PARTIAL} — some lines have stock, not all fully.</li>
 *       <li>{@code REJECT_NO_STOCK} — no line has any available stock.</li>
 *     </ul>
 *   </li>
 *   <li>Return the result — no Order is created, no saga is started, no events published.</li>
 * </ol>
 *
 * <h2>Advisory nature</h2>
 * <p>The result is non-binding. Stock levels may change between the pre-check and the
 * actual reservation. If the real reservation later fails, existing saga compensation
 * paths handle the rollback.
 *
 * <p>Layer: application/service
 * <p>Module: order-service
 */
public class PrepareCorporateBulkOrderService implements PrepareCorporateBulkOrderUseCase {

    private static final Logger log = LoggerFactory.getLogger(PrepareCorporateBulkOrderService.class);

    private final InventoryAvailabilityPort inventoryAvailabilityPort;

    public PrepareCorporateBulkOrderService(final InventoryAvailabilityPort inventoryAvailabilityPort) {
        this.inventoryAvailabilityPort = Require.notNull(inventoryAvailabilityPort, "inventoryAvailabilityPort");
    }

    @Override
    public PrepareCorporateBulkOrderResult prepare(final PrepareCorporateBulkOrderCommand command) {
        Require.notNull(command, "command");

        final List<AvailabilityLineQuery> queries = buildQueries(command);
        final Map<String, Integer> availabilityBySku = fetchAvailability(queries);
        final List<PrepareCorporateBulkOrderLineResult> lineResults = computeLineResults(command, availabilityBySku);

        final List<PrepareCorporateBulkOrderLineResult> availableLines = filterAvailableLines(lineResults);
        final List<PrepareCorporateBulkOrderLineResult> backorderLines = filterBackorderLines(lineResults);
        final boolean allAvailable = lineResults.stream().allMatch(PrepareCorporateBulkOrderLineResult::fullyAvailable);
        final RecommendedAction recommendedAction = determineAction(allAvailable, availableLines);

        log.info("Bulk order preparation customerId={} lines={} action={}",
                command.customerId(), lineResults.size(), recommendedAction);

        return new PrepareCorporateBulkOrderResult(
                allAvailable, lineResults, availableLines, backorderLines, recommendedAction);
    }

    private List<AvailabilityLineQuery> buildQueries(final PrepareCorporateBulkOrderCommand command) {
        return command.lines().stream()
                .map(l -> new AvailabilityLineQuery(l.sku(), l.requestedQuantity()))
                .toList();
    }

    private Map<String, Integer> fetchAvailability(final List<AvailabilityLineQuery> queries) {
        return inventoryAvailabilityPort.checkAvailability(queries).stream()
                .collect(Collectors.toMap(AvailabilityLineResponse::sku, AvailabilityLineResponse::availableQuantity));
    }

    private List<PrepareCorporateBulkOrderLineResult> computeLineResults(
            final PrepareCorporateBulkOrderCommand command, final Map<String, Integer> availabilityBySku) {

        return command.lines().stream()
                .map(line -> {
                    final int stock = availabilityBySku.getOrDefault(line.sku(), 0);
                    final int available = Math.min(stock, line.requestedQuantity());
                    final int backorder = Math.max(0, line.requestedQuantity() - stock);
                    final boolean fullyAvailable = stock >= line.requestedQuantity();
                    return new PrepareCorporateBulkOrderLineResult(
                            line.sku(), line.requestedQuantity(), available, backorder, fullyAvailable);
                })
                .toList();
    }

    private List<PrepareCorporateBulkOrderLineResult> filterAvailableLines(
            final List<PrepareCorporateBulkOrderLineResult> lines) {
        return lines.stream()
                .filter(l -> l.availableQuantity() > 0)
                .toList();
    }

    private List<PrepareCorporateBulkOrderLineResult> filterBackorderLines(
            final List<PrepareCorporateBulkOrderLineResult> lines) {
        return lines.stream()
                .filter(l -> l.backorderQuantity() > 0)
                .toList();
    }

    private RecommendedAction determineAction(
            final boolean allAvailable,
            final List<PrepareCorporateBulkOrderLineResult> availableLines) {

        if (allAvailable) {
            return RecommendedAction.PROCEED_FULL;
        }
        if (!availableLines.isEmpty()) {
            return RecommendedAction.ASK_CUSTOMER_ACCEPT_PARTIAL;
        }
        return RecommendedAction.REJECT_NO_STOCK;
    }
}
