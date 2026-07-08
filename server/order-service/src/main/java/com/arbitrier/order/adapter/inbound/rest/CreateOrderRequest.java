package com.arbitrier.order.adapter.inbound.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * REST request DTO for submitting a corporate bulk order.
 *
 * <p>Layer: adapter/inbound/rest
 * <p>Module: order-service
 */
public record CreateOrderRequest(
        @NotBlank String customerId,
        @NotBlank String submittedByUserId,
        @NotEmpty @Valid List<CreateOrderLineRequest> lines) {}
