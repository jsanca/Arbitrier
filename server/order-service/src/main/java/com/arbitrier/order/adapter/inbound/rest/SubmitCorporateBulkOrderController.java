package com.arbitrier.order.adapter.inbound.rest;

import com.arbitrier.order.application.port.inbound.SubmitCorporateBulkOrderCommand;
import com.arbitrier.order.application.port.inbound.SubmitCorporateBulkOrderLineCommand;
import com.arbitrier.order.application.port.inbound.SubmitCorporateBulkOrderResult;
import com.arbitrier.order.application.port.inbound.SubmitCorporateBulkOrderUseCase;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST adapter that accepts order submission requests and delegates to
 * {@link SubmitCorporateBulkOrderUseCase}.
 *
 * <p>The authenticated user identity ({@code submittedByUserId}) is derived exclusively
 * from the JWT subject claim. The request body does not accept a user identifier — this
 * prevents identity spoofing (ARB-010).
 *
 * <p>Layer: adapter/inbound/rest
 * <p>Module: order-service
 */
@RestController
@RequestMapping("/api/orders")
public class SubmitCorporateBulkOrderController {

    private static final Logger log = LoggerFactory.getLogger(SubmitCorporateBulkOrderController.class);

    private final SubmitCorporateBulkOrderUseCase useCase;
    private final CreateOrderRestMapper createOrderRestMapper;

    public SubmitCorporateBulkOrderController(final SubmitCorporateBulkOrderUseCase useCase,
                                              final CreateOrderRestMapper createOrderRestMapper) {
        this.useCase = useCase;
        this.createOrderRestMapper = createOrderRestMapper;
    }

    /**
     * Submits a new corporate bulk order.
     *
     * <p>The submitting user is taken from the JWT subject — {@code authentication.getName()}.
     * The request body must not contain a {@code submittedByUserId} field.
     *
     * @param request        the order submission payload (customer + lines)
     * @param authentication the verified JWT authentication; injected by Spring Security
     * @return {@code 201 Created} with the new order ID and status
     */
    @PostMapping
    public ResponseEntity<CreateOrderResponse> submitOrder(
            @RequestBody @Valid final CreateOrderRequest request,
            final Authentication authentication) {

        final String userId = authentication.getName();
        log.info("Order submission: customerId={}, lines={}", request.customerId(), request.lines().size());

        final SubmitCorporateBulkOrderCommand command = this.createOrderRestMapper.toCommand(request, userId);

        final SubmitCorporateBulkOrderResult result = this.useCase.execute(command);

        log.info("Order submitted: orderId={}", result.orderId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new CreateOrderResponse(result.orderId(), result.status()));
    }
}
