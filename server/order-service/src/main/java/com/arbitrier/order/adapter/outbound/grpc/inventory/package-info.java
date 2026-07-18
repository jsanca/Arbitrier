/**
 * Outbound gRPC adapter from Order Service to Inventory Service.
 *
 * <p>Implements {@link com.arbitrier.order.application.port.outbound.InventoryAvailabilityPort}
 * using the generated {@code InventoryAvailabilityService} blocking stub.
 * Protobuf types are confined to this package and do not leak into the application layer.
 *
 * <p>Layer: adapter/outbound/grpc/inventory
 * <p>Module: order-service
 */
package com.arbitrier.order.adapter.outbound.grpc.inventory;
