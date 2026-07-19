package com.arbitrier.order.adapter.inbound.rest;

import com.arbitrier.order.adapter.outbound.grpc.inventory.InventoryAvailabilityInternalException;
import com.arbitrier.order.adapter.outbound.grpc.inventory.InventoryAvailabilityProtocolException;
import com.arbitrier.order.adapter.outbound.grpc.inventory.InventoryAvailabilityRemoteUnavailableException;
import com.arbitrier.order.adapter.outbound.grpc.inventory.InventoryAvailabilityTimeoutException;
import com.arbitrier.order.application.OrderProblemCode;
import com.arbitrier.order.application.port.inbound.SubmitCorporateBulkOrderCommand;
import com.arbitrier.order.application.port.inbound.SubmitCorporateBulkOrderResult;
import com.arbitrier.order.application.port.inbound.SubmitCorporateBulkOrderUseCase;
import com.arbitrier.order.integration.OrderServiceTestConfiguration;
import com.arbitrier.platform.error.ApplicationProblemException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-level test for {@link SubmitCorporateBulkOrderController}.
 *
 * <p>Uses a full Spring application context with a mock web environment so the Spring
 * Security filter chain is active. The use case and customer access port are replaced
 * with mocks/stubs so no persistence or event publishing occurs.
 *
 * <p>Verifies HTTP status codes, JWT-based user identity extraction, and the
 * absence of {@code submittedByUserId} from the request payload (ARB-010).
 */
@SpringBootTest(
        webEnvironment = WebEnvironment.MOCK,
        properties = {
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
                        "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration"
        }
)
@Import(OrderServiceTestConfiguration.class)
class SubmitCorporateBulkOrderControllerTest {

    @MockitoBean
    SubmitCorporateBulkOrderUseCase useCase;

    @Autowired
    WebApplicationContext webApplicationContext;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    // ── authentication ────────────────────────────────────────────────────────

    @Test
    void unauthenticated_request_returns_401() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticated_request_returns_201() throws Exception {
        when(useCase.execute(any())).thenReturn(new SubmitCorporateBulkOrderResult("order-abc", "PENDING"));

        mockMvc.perform(post("/api/orders")
                        .with(jwt().jwt(j -> j.subject("user-123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value("order-abc"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    // ── identity extraction ───────────────────────────────────────────────────

    @Test
    void submitted_by_user_id_comes_from_jwt_subject_not_request_body() throws Exception {
        when(useCase.execute(any())).thenReturn(new SubmitCorporateBulkOrderResult("order-1", "PENDING"));

        mockMvc.perform(post("/api/orders")
                        .with(jwt().jwt(j -> j.subject("jwt-user-456")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isCreated());

        ArgumentCaptor<SubmitCorporateBulkOrderCommand> captor =
                ArgumentCaptor.forClass(SubmitCorporateBulkOrderCommand.class);
        verify(useCase).execute(captor.capture());

        assertThat(captor.getValue().submittedByUserId()).isEqualTo("jwt-user-456");
    }

    @Test
    void request_body_with_extra_field_cannot_spoof_user_id() throws Exception {
        when(useCase.execute(any())).thenReturn(new SubmitCorporateBulkOrderResult("order-1", "PENDING"));

        // Body includes a spurious submittedByUserId — Jackson ignores unknown fields;
        // the controller must use the JWT subject, not this body field.
        String bodyWithSpoofedUser = """
                {
                  "customerId": "CUST-001",
                  "submittedByUserId": "spoofed-user",
                  "lines": [{"sku": "SKU-001", "quantity": 5}]
                }
                """;

        mockMvc.perform(post("/api/orders")
                        .with(jwt().jwt(j -> j.subject("jwt-user-789")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithSpoofedUser))
                .andExpect(status().isCreated());

        ArgumentCaptor<SubmitCorporateBulkOrderCommand> captor =
                ArgumentCaptor.forClass(SubmitCorporateBulkOrderCommand.class);
        verify(useCase).execute(captor.capture());

        assertThat(captor.getValue().submittedByUserId())
                .isEqualTo("jwt-user-789")
                .isNotEqualTo("spoofed-user");
    }

    // ── customer access control ───────────────────────────────────────────────

    @Test
    void customer_access_denied_returns_403() throws Exception {
        when(useCase.execute(any()))
                .thenThrow(new ApplicationProblemException(
                        OrderProblemCode.CUSTOMER_ACCESS_DENIED,
                        OrderProblemCode.CUSTOMER_ACCESS_DENIED.description()));

        mockMvc.perform(post("/api/orders")
                        .with(jwt().jwt(j -> j.subject("user-123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ORDER_ACCESS_DENIED"));
    }

    // ── inventory availability ────────────────────────────────────────────────

    @Test
    void inventory_unavailable_returns_422() throws Exception {
        when(useCase.execute(any()))
                .thenThrow(new ApplicationProblemException(
                        OrderProblemCode.ORDER_ITEMS_UNAVAILABLE,
                        OrderProblemCode.ORDER_ITEMS_UNAVAILABLE.description()));

        mockMvc.perform(post("/api/orders")
                        .with(jwt().jwt(j -> j.subject("user-123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ORDER_ITEMS_UNAVAILABLE"));
    }

    @Test
    void inventory_timeout_returns_504() throws Exception {
        when(useCase.execute(any()))
                .thenThrow(new InventoryAvailabilityTimeoutException("timed out", new RuntimeException()));

        mockMvc.perform(post("/api/orders")
                        .with(jwt().jwt(j -> j.subject("user-123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.code").value("INVENTORY_TIMEOUT"));
    }

    @Test
    void inventory_remote_unavailable_returns_503() throws Exception {
        when(useCase.execute(any()))
                .thenThrow(new InventoryAvailabilityRemoteUnavailableException("down", new RuntimeException()));

        mockMvc.perform(post("/api/orders")
                        .with(jwt().jwt(j -> j.subject("user-123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("INVENTORY_SERVICE_UNAVAILABLE"));
    }

    @Test
    void inventory_protocol_error_returns_502() throws Exception {
        when(useCase.execute(any()))
                .thenThrow(new InventoryAvailabilityProtocolException("protocol violation", new RuntimeException()));

        mockMvc.perform(post("/api/orders")
                        .with(jwt().jwt(j -> j.subject("user-123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("INVENTORY_PROTOCOL_ERROR"));
    }

    @Test
    void inventory_internal_error_returns_500() throws Exception {
        when(useCase.execute(any()))
                .thenThrow(new InventoryAvailabilityInternalException("internal error", new RuntimeException()));

        mockMvc.perform(post("/api/orders")
                        .with(jwt().jwt(j -> j.subject("user-123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INVENTORY_INTEGRATION_ERROR"));
    }

    // ── validation ────────────────────────────────────────────────────────────

    @Test
    void missing_customer_id_returns_400() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .with(jwt().jwt(j -> j.subject("user-123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lines": [{"sku": "SKU-001", "quantity": 1}]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void empty_lines_returns_400() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .with(jwt().jwt(j -> j.subject("user-123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": "CUST-001", "lines": []}
                                """))
                .andExpect(status().isBadRequest());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static String validRequestBody() {
        return """
                {
                  "customerId": "CUST-001",
                  "lines": [{"sku": "SKU-001", "quantity": 5}]
                }
                """;
    }
}
