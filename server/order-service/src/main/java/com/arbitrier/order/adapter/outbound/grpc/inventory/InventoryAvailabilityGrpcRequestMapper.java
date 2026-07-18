package com.arbitrier.order.adapter.outbound.grpc.inventory;

import com.arbitrier.contracts.inventory.v1.CheckAvailabilityRequest;
import com.arbitrier.contracts.inventory.v1.RequestedItem;
import com.arbitrier.order.application.port.outbound.AvailabilityLineQuery;

import java.util.List;
import java.util.UUID;

/**
 * Maps Order application availability queries to the gRPC {@code CheckAvailabilityRequest}.
 *
 * <p>Generates a correlation {@code request_id} per invocation. The query list is mapped
 * one-to-one — no aggregation or deduplication is performed here.
 *
 * <p>Layer: adapter/outbound/grpc/inventory
 * <p>Module: order-service
 */
public class InventoryAvailabilityGrpcRequestMapper {

    /**
     * Converts a list of availability queries to a protobuf request.
     *
     * @param lines the non-null, non-empty list of SKU queries
     * @return a fully populated {@code CheckAvailabilityRequest}
     */
    public CheckAvailabilityRequest toRequest(final List<AvailabilityLineQuery> lines) {
        final CheckAvailabilityRequest.Builder builder = CheckAvailabilityRequest.newBuilder()
                .setRequestId(UUID.randomUUID().toString());
        for (final AvailabilityLineQuery line : lines) {
            builder.addItems(RequestedItem.newBuilder()
                    .setProductId(line.sku())
                    .setQuantity(line.requestedQuantity())
                    .build());
        }
        return builder.build();
    }
}
