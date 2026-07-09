package com.arbitrier.order.adapter.inbound.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;


/**
 * REST request DTO for submitting a corporate bulk order.
 *
 * <p>{@code submittedByUserId} is intentionally absent. The user identity is derived
 * from the authenticated JWT subject — it must not be accepted from the request body
 * to prevent identity spoofing (ARB-010).
 *
 * <p>Layer: adapter/inbound/rest
 * <p>Module: order-service
 */
public record CreateOrderRequest(
        @NotBlank String customerId,
        @NotEmpty @Valid List<CreateOrderLineRequest> lines) {}
