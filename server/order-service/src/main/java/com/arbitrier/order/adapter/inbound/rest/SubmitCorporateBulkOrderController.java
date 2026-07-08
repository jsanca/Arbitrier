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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST adapter that accepts order submission requests and delegates to
 * {@link SubmitCorporateBulkOrderUseCase}.
 *
 * <p>Layer: adapter/inbound/rest
 * <p>Module: order-service
 */
@RestController
@RequestMapping("/api/orders")
public class SubmitCorporateBulkOrderController {

    private static final Logger log = LoggerFactory.getLogger(SubmitCorporateBulkOrderController.class);

    private final SubmitCorporateBulkOrderUseCase useCase;

    public SubmitCorporateBulkOrderController(SubmitCorporateBulkOrderUseCase useCase) {
        this.useCase = useCase;
    }

    /**
     * Submits a new corporate bulk order.
     *
     * @param request the order submission payload
     * @return {@code 201 Created} with the new order ID and status
     */
    @PostMapping
    public ResponseEntity<CreateOrderResponse> submitOrder(@RequestBody @Valid CreateOrderRequest request) {
        log.info("Received order submission: customerId={}, lines={}", request.customerId(), request.lines().size());

        List<SubmitCorporateBulkOrderLineCommand> lineCommands = request.lines().stream()
                .map(l -> new SubmitCorporateBulkOrderLineCommand(l.sku(), l.quantity()))
                .toList();

        SubmitCorporateBulkOrderCommand command = new SubmitCorporateBulkOrderCommand(
                request.customerId(),
                request.submittedByUserId(),
                lineCommands);

        SubmitCorporateBulkOrderResult result = useCase.execute(command);

        log.info("Order submitted successfully: orderId={}", result.orderId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new CreateOrderResponse(result.orderId(), result.status()));
    }
}
