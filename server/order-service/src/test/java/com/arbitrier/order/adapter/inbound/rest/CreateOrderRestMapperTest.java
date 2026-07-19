package com.arbitrier.order.adapter.inbound.rest;

import com.arbitrier.order.application.port.inbound.SubmitCorporateBulkOrderCommand;
import com.arbitrier.order.application.port.inbound.SubmitCorporateBulkOrderResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class CreateOrderRestMapperTest {

    private final CreateOrderRestMapper mapper = new CreateOrderRestMapper();

    // ── toCommand — happy path ─────────────────────────────────────────────────

    @Test
    void toCommand_maps_request_to_command() {
        CreateOrderRequest request = new CreateOrderRequest(
                "CUST-001",
                List.of(new CreateOrderLineRequest("SKU-A", 5)));

        SubmitCorporateBulkOrderCommand command = mapper.toCommand(request, "user-001");

        assertThat(command.customerId()).isEqualTo("CUST-001");
        assertThat(command.submittedByUserId()).isEqualTo("user-001");
        assertThat(command.lines()).hasSize(1);
        assertThat(command.lines().get(0).sku()).isEqualTo("SKU-A");
        assertThat(command.lines().get(0).quantity()).isEqualTo(5);
    }

    @Test
    void toCommand_maps_multiple_lines() {
        CreateOrderRequest request = new CreateOrderRequest(
                "CUST-001",
                List.of(
                        new CreateOrderLineRequest("SKU-A", 5),
                        new CreateOrderLineRequest("SKU-B", 3)));

        SubmitCorporateBulkOrderCommand command = mapper.toCommand(request, "user-001");

        assertThat(command.lines()).hasSize(2);
        assertThat(command.lines()).extracting("sku").containsExactly("SKU-A", "SKU-B");
        assertThat(command.lines()).extracting("quantity").containsExactly(5, 3);
    }

    // ── toCommand — null / invalid input ─────────────────────────────────────

    @Test
    void toCommand_rejects_null_request() {
        assertThatNullPointerException()
                .isThrownBy(() -> mapper.toCommand(null, "user-001"))
                .withMessageContaining("request");
    }

    @Test
    void toCommand_rejects_null_customerId() {
        CreateOrderRequest request = new CreateOrderRequest(null, List.of());

        assertThatNullPointerException()
                .isThrownBy(() -> mapper.toCommand(request, "user-001"))
                .withMessageContaining("customerId");
    }

    @Test
    void toCommand_rejects_blank_customerId() {
        CreateOrderRequest request = new CreateOrderRequest("   ", List.of());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> mapper.toCommand(request, "user-001"))
                .withMessageContaining("customerId")
                .withMessageContaining("blank");
    }

    @Test
    void toCommand_rejects_null_submittedByUserId() {
        CreateOrderRequest request = new CreateOrderRequest("CUST-001", List.of());

        assertThatNullPointerException()
                .isThrownBy(() -> mapper.toCommand(request, null))
                .withMessageContaining("submittedByUserId");
    }

    @Test
    void toCommand_rejects_blank_submittedByUserId() {
        CreateOrderRequest request = new CreateOrderRequest("CUST-001", List.of());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> mapper.toCommand(request, "   "))
                .withMessageContaining("submittedByUserId")
                .withMessageContaining("blank");
    }

    @Test
    void toCommand_rejects_null_lines() {
        CreateOrderRequest request = new CreateOrderRequest("CUST-001", null);

        assertThatNullPointerException()
                .isThrownBy(() -> mapper.toCommand(request, "user-001"))
                .withMessageContaining("lines");
    }

    @Test
    void toCommand_rejects_empty_lines() {
        CreateOrderRequest request = new CreateOrderRequest("CUST-001", List.of());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> mapper.toCommand(request, "user-001"))
                .withMessageContaining("lines")
                .withMessageContaining("empty");
    }

    // ── toResponse — happy path ────────────────────────────────────────────────

    @Test
    void toResponse_maps_result_to_response() {
        SubmitCorporateBulkOrderResult result = new SubmitCorporateBulkOrderResult(
                "order-001", "CONFIRMED");

        CreateOrderResponse response = mapper.toResponse(result);

        assertThat(response.orderId()).isEqualTo("order-001");
        assertThat(response.status()).isEqualTo("CONFIRMED");
    }

    // ── toResponse — null / invalid input ────────────────────────────────────

    @Test
    void toResponse_rejects_null_result() {
        assertThatNullPointerException()
                .isThrownBy(() -> mapper.toResponse(null))
                .withMessageContaining("result");
    }

    @Test
    void toResponse_rejects_null_orderId() {
        SubmitCorporateBulkOrderResult result = new SubmitCorporateBulkOrderResult(null, "CONFIRMED");

        assertThatNullPointerException()
                .isThrownBy(() -> mapper.toResponse(result))
                .withMessageContaining("orderId");
    }

    @Test
    void toResponse_rejects_blank_orderId() {
        SubmitCorporateBulkOrderResult result = new SubmitCorporateBulkOrderResult("   ", "CONFIRMED");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> mapper.toResponse(result))
                .withMessageContaining("orderId")
                .withMessageContaining("blank");
    }

    @Test
    void toResponse_rejects_null_status() {
        SubmitCorporateBulkOrderResult result = new SubmitCorporateBulkOrderResult("order-001", null);

        assertThatNullPointerException()
                .isThrownBy(() -> mapper.toResponse(result))
                .withMessageContaining("status");
    }

    @Test
    void toResponse_rejects_blank_status() {
        SubmitCorporateBulkOrderResult result = new SubmitCorporateBulkOrderResult("order-001", "   ");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> mapper.toResponse(result))
                .withMessageContaining("status")
                .withMessageContaining("blank");
    }
}
