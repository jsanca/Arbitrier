package com.arbitrier.order.adapter.inbound.rest;

import com.arbitrier.order.application.port.inbound.SubmitCorporateBulkOrderCommand;
import com.arbitrier.order.application.port.inbound.SubmitCorporateBulkOrderLineCommand;
import com.arbitrier.order.application.port.inbound.SubmitCorporateBulkOrderResult;
import com.arbitrier.platform.validation.Require;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maps REST request/response DTOs to application command and result objects.
 *
 * <p>This mapper bridges the inbound REST adapter layer and the application
 * layer, translating {@link CreateOrderRequest} into
 * {@link SubmitCorporateBulkOrderCommand} and {@link SubmitCorporateBulkOrderResult}
 * into {@link CreateOrderResponse}.
 *
 * <p>Validation of REST input is handled by Bean Validation annotations on the
 * request records ({@code @NotBlank}, {@code @NotEmpty}, {@code @Min}) prior to
 * mapping. The mapper itself assumes inputs are non-null unless documented
 * otherwise.
 *
 * <p>Layer: adapter/inbound/rest
 * <p>Module: order-service
 */
@Component
final public class CreateOrderRestMapper {

    /**
     * Maps a create order REST request to the corresponding application command.
     *
     * <p>The {@code submittedByUserId} is injected by the controller from the
     * authenticated JWT subject — it must not come from the request body
     * (ARB-010 anti-spoofing requirement).
     *
     * <p>Validation errors (null, blank, empty) thrown by this method originate
     * from the {@link SubmitCorporateBulkOrderCommand} record constructor.
     *
     * @param request          the REST request containing customer and lines; must not be null
     * @param submittedByUserId the user identity derived from the JWT; must not be blank
     * @return the application command for submission
     * @throws NullPointerException           if any argument is null
     * @throws IllegalArgumentException        if any string argument is blank or collection is empty
     */
    SubmitCorporateBulkOrderCommand toCommand(
            final CreateOrderRequest request,
            final String submittedByUserId) {

        return new SubmitCorporateBulkOrderCommand(
                request.customerId(),
                submittedByUserId,
                this.toLineCommands(request.lines()));
    }

    private List<SubmitCorporateBulkOrderLineCommand> toLineCommands(
            final List<CreateOrderLineRequest> lines) {

        return lines.stream()
                .map(this::toLineCommand)
                .toList();
    }

    private SubmitCorporateBulkOrderLineCommand toLineCommand(
            final CreateOrderLineRequest line) {

        return new SubmitCorporateBulkOrderLineCommand(
                line.sku(),
                line.quantity());
    }

    /**
     * Maps an application result to the corresponding REST response.
     *
     * <p>Both {@code orderId} and {@code status} are passed through unchanged.
     *
     * @param result the application result; must not be null
     * @return the REST response DTO
     * @throws NullPointerException           if {@code result} is null
     * @throws IllegalArgumentException        if {@code orderId} or {@code status} is blank
     */
    CreateOrderResponse toResponse(
            final SubmitCorporateBulkOrderResult result) {

        Require.notNull(result, "result");
        return new CreateOrderResponse(
                Require.notBlank(result.orderId(), "orderId"),
                Require.notBlank(result.status(), "status"));
    }
}
