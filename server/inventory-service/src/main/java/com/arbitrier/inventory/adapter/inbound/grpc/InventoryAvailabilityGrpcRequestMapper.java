package com.arbitrier.inventory.adapter.inbound.grpc;

import com.arbitrier.contracts.inventory.v1.CheckAvailabilityRequest;
import com.arbitrier.inventory.application.port.inbound.InventoryAvailabilityQuery;
import com.arbitrier.inventory.application.port.inbound.RequestedInventoryItem;

import java.util.List;

/**
 * Maps a {@link CheckAvailabilityRequest} protobuf message to an application-layer
 * {@link InventoryAvailabilityQuery}.
 *
 * <p>No protobuf type may cross into the application or domain layer.
 *
 * <p>Layer: adapter/inbound/grpc
 * <p>Module: inventory-service
 */
public class InventoryAvailabilityGrpcRequestMapper {

    /**
     * Maps a protobuf availability request to the application query.
     *
     * @param request the incoming protobuf request
     * @return the application query
     * @throws IllegalArgumentException if any field fails semantic validation
     */
    public InventoryAvailabilityQuery toQuery(final CheckAvailabilityRequest request) {
        final List<RequestedInventoryItem> items = request.getItemsList().stream()
                .map(item -> new RequestedInventoryItem(item.getProductId(), item.getQuantity()))
                .toList();
        return new InventoryAvailabilityQuery(request.getRequestId(), items);
    }
}
