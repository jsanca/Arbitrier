package com.arbitrier.inventory.application.service;

import com.arbitrier.inventory.application.port.inbound.CheckInventoryAvailabilityUseCase;
import com.arbitrier.inventory.application.port.inbound.InventoryAvailabilityQuery;
import com.arbitrier.inventory.application.port.inbound.InventoryAvailabilityResult;
import com.arbitrier.inventory.application.port.inbound.InventoryUnavailabilityReason;
import com.arbitrier.inventory.application.port.inbound.RequestedInventoryItem;
import com.arbitrier.inventory.application.port.inbound.UnavailableInventoryItem;
import com.arbitrier.inventory.application.port.outbound.InventoryAvailabilityQueryPort;
import com.arbitrier.inventory.application.port.outbound.InventoryAvailabilitySnapshot;
import com.arbitrier.platform.validation.Require;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Evaluates whether all requested products currently have sufficient available stock.
 *
 * <p>Evaluates every item in the query and collects all failures — does not stop at the first.
 * This is a read-only operation; no stock is reserved, decremented, or locked.
 *
 * <p>Layer: application/service
 * <p>Module: inventory-service
 */
public class CheckInventoryAvailabilityService implements CheckInventoryAvailabilityUseCase {

    private static final Logger log = LoggerFactory.getLogger(CheckInventoryAvailabilityService.class);

    private final InventoryAvailabilityQueryPort queryPort;

    public CheckInventoryAvailabilityService(final InventoryAvailabilityQueryPort queryPort) {
        this.queryPort = Require.notNull(queryPort, "CheckInventoryAvailabilityService.queryPort");
    }

    @Override
    public InventoryAvailabilityResult check(final InventoryAvailabilityQuery query) {
        Require.notNull(query, "CheckInventoryAvailabilityService.query");
        validateNoDuplicates(query.items());

        log.debug("Checking inventory availability: requestId={}, itemCount={}",
                query.requestId(), query.items().size());

        final List<String> productIds = query.items().stream()
                .map(RequestedInventoryItem::productId)
                .toList();

        final Map<String, InventoryAvailabilitySnapshot> snapshots =
                queryPort.findAvailability(productIds);

        final List<UnavailableInventoryItem> unavailableItems = evaluateItems(query.items(), snapshots);

        if (unavailableItems.isEmpty()) {
            log.debug("Availability check passed: requestId={}", query.requestId());
            return new InventoryAvailabilityResult.Available();
        }

        log.debug("Availability check failed: requestId={}, unavailableCount={}",
                query.requestId(), unavailableItems.size());
        return new InventoryAvailabilityResult.Unavailable(unavailableItems);
    }

    private List<UnavailableInventoryItem> evaluateItems(
            final List<RequestedInventoryItem> items,
            final Map<String, InventoryAvailabilitySnapshot> snapshots) {

        final List<UnavailableInventoryItem> unavailable = new ArrayList<>();

        for (final RequestedInventoryItem item : items) {
            final InventoryAvailabilitySnapshot snapshot = snapshots.get(item.productId());

            if (snapshot == null) {
                unavailable.add(new UnavailableInventoryItem(
                        item.productId(),
                        item.quantity(),
                        0,
                        InventoryUnavailabilityReason.PRODUCT_NOT_FOUND));
            } else if (snapshot.availableQuantity() < item.quantity()) {
                unavailable.add(new UnavailableInventoryItem(
                        item.productId(),
                        item.quantity(),
                        snapshot.availableQuantity(),
                        InventoryUnavailabilityReason.INSUFFICIENT_STOCK));
            }
        }

        return unavailable;
    }

    private void validateNoDuplicates(final List<RequestedInventoryItem> items) {
        final Set<String> seen = new HashSet<>();
        for (final RequestedInventoryItem item : items) {
            if (!seen.add(item.productId())) {
                throw new IllegalArgumentException(
                        "Duplicate product ID in availability query: " + item.productId());
            }
        }
    }
}
