package com.arbitrier.order.config;

import com.arbitrier.contracts.inventory.v1.InventoryAvailabilityServiceGrpc;
import com.arbitrier.order.adapter.outbound.grpc.inventory.GrpcInventoryAvailabilityAdapter;
import com.arbitrier.order.application.port.outbound.InventoryAvailabilityPort;
import net.devh.boot.grpc.client.channelfactory.GrpcChannelFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Spring configuration for the Order Service gRPC client targeting Inventory Service.
 *
 * <p>Conditional on {@code grpc.client.inventory.address} being set — the same pattern
 * used for Kafka ({@code spring.kafka.bootstrap-servers}). Tests that do not configure
 * this property skip this class entirely; test stubs fill the port instead. In production
 * the address is supplied via the {@code INVENTORY_GRPC_ADDRESS} environment variable.
 *
 * <p>Target channel is named {@code "inventory"} and is configured by
 * {@code grpc.client.inventory.address} and {@code grpc.client.inventory.negotiation-type}.
 *
 * <p>Layer: config
 * <p>Module: order-service
 */
@Configuration
@ConditionalOnProperty(name = "grpc.client.inventory.address")
public class OrderInventoryGrpcConfiguration {

    /**
     * Creates the blocking stub from the {@code "inventory"} channel registered with the
     * {@code net.devh} starter. The channel is managed by the starter lifecycle.
     */
    @Bean
    InventoryAvailabilityServiceGrpc.InventoryAvailabilityServiceBlockingStub inventoryGrpcStub(
            final GrpcChannelFactory channelFactory) {
        return InventoryAvailabilityServiceGrpc.newBlockingStub(
                channelFactory.createChannel("inventory"));
    }

    /**
     * Wraps the blocking stub in the Order outbound adapter with a configured deadline.
     */
    @Bean
    InventoryAvailabilityPort inventoryAvailabilityPort(
            final InventoryAvailabilityServiceGrpc.InventoryAvailabilityServiceBlockingStub stub,
            @Value("${arbitrier.order.inventory.grpc.deadline-ms:2000}") final long deadlineMs) {
        return new GrpcInventoryAvailabilityAdapter(stub, Duration.ofMillis(deadlineMs));
    }
}
