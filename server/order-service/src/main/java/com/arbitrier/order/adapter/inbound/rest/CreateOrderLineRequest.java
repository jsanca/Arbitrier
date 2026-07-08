package com.arbitrier.order.adapter.inbound.rest;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * REST request DTO for a single order line.
 *
 * <p>Layer: adapter/inbound/rest
 * <p>Module: order-service
 */
public record CreateOrderLineRequest(
        @NotBlank String sku,
        @Min(1) int quantity) {}
