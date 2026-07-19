package com.arbitrier.order.integration;

import com.arbitrier.order.adapter.outbound.InMemoryOrderRepository;
import com.arbitrier.order.adapter.outbound.StubInventoryAvailabilityPort;
import com.arbitrier.order.application.port.outbound.CustomerAccessPort;
import com.arbitrier.order.application.port.outbound.InventoryAvailabilityPort;
import com.arbitrier.order.application.port.outbound.OrderRepository;
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.test.InMemoryOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration test for corporate bulk order creation.
 *
 * <p>Wires the real {@link com.arbitrier.order.application.service.SubmitCorporateBulkOrderService}
 * through the REST adapter; all outbound ports are in-memory test doubles. No mocks on the
 * use case — exercises the full HTTP → controller → service → port path.
 *
 * <p>Layer: integration
 * <p>Module: order-service
 */
@SpringBootTest(
        webEnvironment = WebEnvironment.MOCK,
        properties = {
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
                        "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration," +
                        "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"
        }
)
@Import(OrderServiceTestConfiguration.class)
class OrderCreationIT {

    @MockitoBean
    CustomerAccessPort customerAccessPort;

    @Autowired
    WebApplicationContext webApplicationContext;

    @Autowired
    InventoryAvailabilityPort inventoryAvailabilityPort;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    OutboxRepository outboxRepository;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        when(customerAccessPort.canSubmitOrder(any(), any())).thenReturn(true);
        ((StubInventoryAvailabilityPort) inventoryAvailabilityPort).reset();
        ((InMemoryOrderRepository) orderRepository).clear();
        ((InMemoryOutboxRepository) outboxRepository).clear();
    }

    // ── sufficient inventory ───────────────────────────────────────────────────

    @Test
    void sufficient_inventory_returns_201_with_order_id_and_pending_status() throws Exception {
        ((StubInventoryAvailabilityPort) inventoryAvailabilityPort).setUnlimited("SKU-001");

        mockMvc.perform(post("/api/orders")
                        .with(jwt().jwt(j -> j.subject("user-001")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void sufficient_inventory_persists_order_and_outbox_event() throws Exception {
        ((StubInventoryAvailabilityPort) inventoryAvailabilityPort).setUnlimited("SKU-001");

        mockMvc.perform(post("/api/orders")
                        .with(jwt().jwt(j -> j.subject("user-001")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isCreated());

        InMemoryOutboxRepository typedOutbox = (InMemoryOutboxRepository) outboxRepository;
        assertThat(((InMemoryOrderRepository) orderRepository).size()).isEqualTo(1);
        assertThat(typedOutbox.findAll()).hasSize(1);
        assertThat(typedOutbox.findAll().get(0).eventType()).isEqualTo("OrderCreatedDomainEvent");
    }

    // ── insufficient inventory ─────────────────────────────────────────────────

    @Test
    void insufficient_inventory_returns_422_and_no_order_saved() throws Exception {
        ((StubInventoryAvailabilityPort) inventoryAvailabilityPort).setAvailable("SKU-001", 1); // requested 5

        mockMvc.perform(post("/api/orders")
                        .with(jwt().jwt(j -> j.subject("user-001")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ORDER_ITEMS_UNAVAILABLE"));

        assertThat(((InMemoryOrderRepository) orderRepository).size()).isZero();
        assertThat(((InMemoryOutboxRepository) outboxRepository).findAll()).isEmpty();
    }

    // ── authorization rejection ────────────────────────────────────────────────

    @Test
    void unauthorized_customer_returns_403_and_no_order_saved() throws Exception {
        ((StubInventoryAvailabilityPort) inventoryAvailabilityPort).setUnlimited("SKU-001");
        when(customerAccessPort.canSubmitOrder(any(), any())).thenReturn(false);

        mockMvc.perform(post("/api/orders")
                        .with(jwt().jwt(j -> j.subject("user-001")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ORDER_ACCESS_DENIED"));

        assertThat(((InMemoryOrderRepository) orderRepository).size()).isZero();
        assertThat(((InMemoryOutboxRepository) outboxRepository).findAll()).isEmpty();
    }

    // ── request validation ────────────────────────────────────────────────────

    @Test
    void zero_quantity_returns_400_and_use_case_not_executed() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .with(jwt().jwt(j -> j.subject("user-001")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "CUST-001",
                                  "lines": [{"sku": "SKU-001", "quantity": 0}]
                                }
                                """))
                .andExpect(status().isBadRequest());

        assertThat(((InMemoryOrderRepository) orderRepository).size()).isZero();
        assertThat(((InMemoryOutboxRepository) outboxRepository).findAll()).isEmpty();
    }

    @Test
    void negative_quantity_returns_400_and_use_case_not_executed() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .with(jwt().jwt(j -> j.subject("user-001")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "CUST-001",
                                  "lines": [{"sku": "SKU-001", "quantity": -5}]
                                }
                                """))
                .andExpect(status().isBadRequest());

        assertThat(((InMemoryOrderRepository) orderRepository).size()).isZero();
        assertThat(((InMemoryOutboxRepository) outboxRepository).findAll()).isEmpty();
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
