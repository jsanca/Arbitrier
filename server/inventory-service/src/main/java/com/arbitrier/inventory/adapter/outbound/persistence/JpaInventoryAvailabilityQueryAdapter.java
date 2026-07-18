package com.arbitrier.inventory.adapter.outbound.persistence;

import com.arbitrier.inventory.application.port.outbound.InventoryAvailabilityQueryPort;
import com.arbitrier.inventory.application.port.outbound.InventoryAvailabilitySnapshot;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Production JPA implementation of {@link InventoryAvailabilityQueryPort}.
 *
 * <p>Availability is computed as:
 * <pre>
 *   available = max(0, on_hand_quantity - SUM(active allocation quantities))
 * </pre>
 *
 * <p>Active reservation states that consume availability: {@code RESERVED}, {@code PARTIALLY_RESERVED}.
 * {@code REJECTED} and {@code RELEASED} reservations do not reduce available stock.
 *
 * <p>Two bounded queries are issued per call — one Spring Data batch fetch for stock,
 * one native aggregate query for reserved totals — then merged in Java.
 * No N+1 behavior is introduced regardless of how many product IDs are requested.
 *
 * <p>This adapter is read-only and never mutates stock or reservation data.
 *
 * <p>Layer: adapter/outbound/persistence
 * <p>Module: inventory-service
 */
public class JpaInventoryAvailabilityQueryAdapter implements InventoryAvailabilityQueryPort {

    private static final Logger log = LoggerFactory.getLogger(JpaInventoryAvailabilityQueryAdapter.class);

    /** Reservation statuses that reduce available stock. */
    private static final List<String> ACTIVE_STATUSES = List.of("RESERVED", "PARTIALLY_RESERVED");

    private static final String RESERVED_TOTALS_QUERY = """
            SELECT srl.sku_code, COALESCE(SUM(sa.quantity), 0)
            FROM inventory_service.stock_reservation_lines srl
            JOIN inventory_service.stock_reservations sr ON sr.id = srl.reservation_id
            JOIN inventory_service.stock_allocations sa ON sa.line_id = srl.id
            WHERE sr.status IN :activeStatuses
              AND srl.sku_code IN :skuCodes
            GROUP BY srl.sku_code
            """;

    private final SpringDataInventoryStockRepository stockRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public JpaInventoryAvailabilityQueryAdapter(SpringDataInventoryStockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    /**
     * Retrieves availability snapshots for all known products in the given set.
     *
     * <p>Unknown products are absent from the returned map — callers interpret absence
     * as {@code PRODUCT_NOT_FOUND}.
     *
     * <p>If active reservation allocations exceed on-hand stock due to inconsistent data,
     * available quantity is clamped to zero rather than returning a negative value.
     *
     * @param productIds product identifiers to query
     * @return map of product ID to availability snapshot; excludes unknown products
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, InventoryAvailabilitySnapshot> findAvailability(Collection<String> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }

        List<String> ids = List.copyOf(productIds);

        List<InventoryStockEntity> stockEntities = stockRepository.findAllById(ids);
        Map<String, Integer> reservedByProductId = queryReservedTotals(ids);

        Map<String, InventoryAvailabilitySnapshot> result = new LinkedHashMap<>();
        for (InventoryStockEntity entity : stockEntities) {
            int onHand = entity.getOnHandQuantity();
            int reserved = reservedByProductId.getOrDefault(entity.getProductId(), 0);
            int calculated = onHand - reserved;
            if (calculated < 0) {
                log.warn("Inventory inconsistency for product {}: on_hand={}, reserved={} — clamping to 0",
                        entity.getProductId(), onHand, reserved);
                calculated = 0;
            }
            result.put(entity.getProductId(),
                    new InventoryAvailabilitySnapshot(entity.getProductId(), calculated));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Integer> queryReservedTotals(List<String> productIds) {
        List<Object[]> rows = entityManager.createNativeQuery(RESERVED_TOTALS_QUERY)
                .setParameter("activeStatuses", ACTIVE_STATUSES)
                .setParameter("skuCodes", productIds)
                .getResultList();

        Map<String, Integer> totals = new HashMap<>();
        for (Object[] row : rows) {
            String sku = (String) row[0];
            int reserved = ((Number) row[1]).intValue();
            totals.put(sku, reserved);
        }
        return totals;
    }
}
