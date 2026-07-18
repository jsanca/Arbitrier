package com.arbitrier.order.adapter.outbound.grpc.inventory;

import com.arbitrier.contracts.inventory.v1.AvailabilityStatus;
import com.arbitrier.contracts.inventory.v1.CheckAvailabilityResponse;
import com.arbitrier.contracts.inventory.v1.UnavailableItem;
import com.arbitrier.order.application.port.outbound.AvailabilityLineQuery;
import com.arbitrier.order.application.port.outbound.AvailabilityLineResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Maps gRPC {@code CheckAvailabilityResponse} to the Order application's
 * {@link AvailabilityLineResponse} list.
 *
 * <p>Mapping rules:
 * <ul>
 *   <li>{@code AVAILABLE} — every requested SKU is available; returns each line with
 *       {@code availableQuantity = requestedQuantity}.</li>
 *   <li>{@code UNAVAILABLE} — items absent from the {@code unavailable_items} list were
 *       available (returns {@code requestedQuantity}); items present in the list return
 *       the server-provided {@code available_quantity}.</li>
 *   <li>{@code UNSPECIFIED} — treated as a protocol violation; throws
 *       {@link InventoryAvailabilityProtocolException}.</li>
 * </ul>
 *
 * <p>Layer: adapter/outbound/grpc/inventory
 * <p>Module: order-service
 */
public class InventoryAvailabilityGrpcResponseMapper {

    /**
     * Converts a gRPC availability response to a per-line availability list.
     *
     * <p>The {@code queries} list is required to determine requested quantities for SKUs
     * that the server reported as available (which do not appear in {@code unavailable_items}).
     *
     * @param response the gRPC response from Inventory Service
     * @param queries  the original query list that produced this response
     * @return one {@link AvailabilityLineResponse} per query line
     * @throws InventoryAvailabilityProtocolException if the response status is UNSPECIFIED
     *         or if AVAILABLE is returned alongside non-empty unavailable items
     */
    public List<AvailabilityLineResponse> toLines(
            final CheckAvailabilityResponse response,
            final List<AvailabilityLineQuery> queries) {

        final AvailabilityStatus status = response.getStatus();

        if (status == AvailabilityStatus.AVAILABILITY_STATUS_UNSPECIFIED) {
            throw new InventoryAvailabilityProtocolException(
                    "Inventory returned AVAILABILITY_STATUS_UNSPECIFIED — this is a protocol violation");
        }

        if (status == AvailabilityStatus.AVAILABILITY_STATUS_AVAILABLE) {
            if (!response.getUnavailableItemsList().isEmpty()) {
                throw new InventoryAvailabilityProtocolException(
                        "Inventory returned AVAILABLE with non-empty unavailable_items — protocol violation");
            }
            return queries.stream()
                    .map(q -> new AvailabilityLineResponse(q.sku(), q.requestedQuantity()))
                    .toList();
        }

        // UNAVAILABLE: build a map from product_id to available_quantity
        final Map<String, Integer> unavailableByProductId = response.getUnavailableItemsList().stream()
                .collect(Collectors.toMap(UnavailableItem::getProductId, UnavailableItem::getAvailableQuantity));

        final List<AvailabilityLineResponse> result = new ArrayList<>(queries.size());
        for (final AvailabilityLineQuery query : queries) {
            final int available = unavailableByProductId.containsKey(query.sku())
                    ? unavailableByProductId.get(query.sku())
                    : query.requestedQuantity();
            result.add(new AvailabilityLineResponse(query.sku(), available));
        }
        return result;
    }
}
