package com.arbitrier.order.integration;

import com.arbitrier.contracts.inventory.v1.InventoryAvailabilityServiceGrpc;
import com.arbitrier.inventory.adapter.inbound.grpc.InventoryAvailabilityGrpcRequestMapper;
import com.arbitrier.inventory.adapter.inbound.grpc.InventoryAvailabilityGrpcResponseMapper;
import com.arbitrier.inventory.adapter.inbound.grpc.InventoryAvailabilityGrpcService;
import com.arbitrier.inventory.adapter.inbound.grpc.InventoryGrpcExceptionMapper;
import com.arbitrier.inventory.application.port.inbound.CheckInventoryAvailabilityUseCase;
import com.arbitrier.inventory.application.port.inbound.InventoryAvailabilityResult;
import com.arbitrier.inventory.application.port.inbound.InventoryUnavailabilityReason;
import com.arbitrier.inventory.application.port.inbound.UnavailableInventoryItem;
import com.arbitrier.order.adapter.outbound.grpc.inventory.GrpcInventoryAvailabilityAdapter;
import com.arbitrier.order.application.port.outbound.InventoryAvailabilityPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Vertical integration proof for corporate bulk order submission.
 *
 * <p>Exercises the full production path from HTTP to persistence:
 * <pre>
 *   MockMvc POST /api/orders
 *       → SubmitCorporateBulkOrderController (security, routing)
 *       → CreateOrderRestMapper
 *       → SubmitCorporateBulkOrderService (@Transactional)
 *       → GrpcInventoryAvailabilityAdapter  ← real production adapter
 *       → in-process gRPC channel
 *       → InventoryAvailabilityGrpcService  ← real inventory gRPC service
 *       → JpaOrderRepositoryAdapter         ← real JPA
 *       → JpaOutboxRepositoryAdapter        ← real JPA
 *       → HTTP response
 * </pre>
 *
 * <p><b>Test seams (documented):</b>
 * <ul>
 *   <li>In-process gRPC channel — replaces the real network hop; all serialization and
 *       mapping still runs.</li>
 *   <li>Configurable {@link CheckInventoryAvailabilityUseCase} stub — controls what
 *       the inventory server returns per scenario.</li>
 *   <li>Testcontainers PostgreSQL — replaces the real database; Flyway migrates the schema.</li>
 * </ul>
 *
 * <p>All Spring wiring is the production wiring. The {@link GrpcInventoryAvailabilityAdapter}
 * with its {@code validateResponseContract()} and exception mapping runs unmodified.
 *
 * <p>Layer: integration
 * <p>Module: order-service
 */
@SpringBootTest(
        webEnvironment = WebEnvironment.MOCK,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.open-in-view=false"
        }
)
@Testcontainers
class CorporateBulkOrderVerticalIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withInitScript("test-db/create-order-service-schema.sql");

    @TestConfiguration
    static class Config {

        static volatile Server grpcServer;
        static volatile ManagedChannel grpcChannel;

        /** Swapped per test to configure inventory scenario without restarting the server. */
        static final AtomicReference<CheckInventoryAvailabilityUseCase> STUB =
                new AtomicReference<>(query -> new InventoryAvailabilityResult.Available());

        @Bean
        @Primary
        InventoryAvailabilityPort inventoryAvailabilityPort() throws IOException {
            final String serverName = "vertical-it-" + UUID.randomUUID();
            grpcServer = InProcessServerBuilder.forName(serverName)
                    .directExecutor()
                    .addService(new InventoryAvailabilityGrpcService(
                            query -> Config.STUB.get().check(query),
                            new InventoryAvailabilityGrpcRequestMapper(),
                            new InventoryAvailabilityGrpcResponseMapper(),
                            new InventoryGrpcExceptionMapper()))
                    .build()
                    .start();
            grpcChannel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
            return new GrpcInventoryAvailabilityAdapter(
                    InventoryAvailabilityServiceGrpc.newBlockingStub(grpcChannel),
                    Duration.ofSeconds(5));
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .claim("sub", "test-user")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
        }
    }

    @Autowired
    WebApplicationContext webApplicationContext;

    @Autowired
    JdbcTemplate jdbcTemplate;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        Config.STUB.set(query -> new InventoryAvailabilityResult.Available());
        jdbcTemplate.execute("DELETE FROM order_service.outbox_events");
        jdbcTemplate.execute("DELETE FROM order_service.order_lines");
        jdbcTemplate.execute("DELETE FROM order_service.orders");
    }

    @AfterAll
    static void tearDownGrpc() throws InterruptedException {
        if (Config.grpcChannel != null) {
            Config.grpcChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
        if (Config.grpcServer != null) {
            Config.grpcServer.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    // ── scenario 1: successful submission — duplicate SKU normalization ─────────

    @Test
    void duplicate_skus_normalized_order_and_outbox_persisted() throws Exception {
        final MvcResult result = mockMvc.perform(post("/api/orders")
                        .with(jwt().jwt(j -> j.subject("user-vertical-001")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateSkuBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        final String orderId = new ObjectMapper()
                .readTree(result.getResponse().getContentAsString())
                .path("orderId").asText();

        // Exactly one Order persisted in PENDING state
        assertThat(orderCount()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM order_service.orders WHERE id = ?", String.class, orderId))
                .isEqualTo("PENDING");

        // HTTP orderId matches the persisted row
        assertThat(jdbcTemplate.queryForObject("SELECT id FROM order_service.orders", String.class))
                .isEqualTo(orderId);

        // Normalized lines: SKU-A qty 5 (3+2), SKU-B qty 1
        final List<Map<String, Object>> lines = jdbcTemplate.queryForList(
                "SELECT sku, quantity FROM order_service.order_lines WHERE order_id = ? ORDER BY sku",
                orderId);
        assertThat(lines).hasSize(2);
        assertThat(lines).anySatisfy(l -> {
            assertThat(l.get("sku")).isEqualTo("SKU-A");
            assertThat(((Number) l.get("quantity")).intValue()).isEqualTo(5);
        });
        assertThat(lines).anySatisfy(l -> {
            assertThat(l.get("sku")).isEqualTo("SKU-B");
            assertThat(((Number) l.get("quantity")).intValue()).isEqualTo(1);
        });

        // Exactly one Outbox event
        assertThat(outboxCount()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT event_type FROM order_service.outbox_events", String.class))
                .isEqualTo("OrderCreatedDomainEvent");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT aggregate_id FROM order_service.outbox_events", String.class))
                .isEqualTo(orderId);

        // Outbox payload lines match the normalized quantities
        final String payload = jdbcTemplate.queryForObject(
                "SELECT payload FROM order_service.outbox_events", String.class);
        final JsonNode payloadLines = new ObjectMapper().readTree(payload).path("lines");
        assertThat(payloadLines).hasSize(2);
    }

    // ── scenario 2: business rejection — insufficient inventory via real gRPC ───

    @Test
    void insufficient_inventory_via_grpc_returns_422_and_nothing_persisted() throws Exception {
        Config.STUB.set(query -> new InventoryAvailabilityResult.Unavailable(
                query.items().stream()
                        .map(item -> new UnavailableInventoryItem(
                                item.productId(), item.quantity(), 1,
                                InventoryUnavailabilityReason.INSUFFICIENT_STOCK))
                        .toList()));

        mockMvc.perform(post("/api/orders")
                        .with(jwt().jwt(j -> j.subject("user-vertical-002")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateSkuBody()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ORDER_ITEMS_UNAVAILABLE"));

        assertThat(orderCount()).isZero();
        assertThat(outboxCount()).isZero();
    }

    // ── scenario 3: protocol failure — gRPC INVALID_ARGUMENT → 502 ────────────

    /**
     * The stub use case throws {@link IllegalArgumentException}.
     * This exercises the full production exception path:
     * {@code InventoryGrpcExceptionMapper} → {@code INVALID_ARGUMENT} gRPC status
     * → {@code InventoryAvailabilityGrpcExceptionMapper} → {@code InventoryAvailabilityProtocolException}
     * → {@code OrderInventoryExceptionHandler} → HTTP 502 {@code INVENTORY_PROTOCOL_ERROR}.
     *
     * <p>Test seam: the {@code INVALID_ARGUMENT} originates from the stub use case rather
     * than from a real malformed protobuf frame (which the production mapper cannot produce).
     * All production gRPC-to-exception mapping code runs unmodified.
     */
    @Test
    void grpc_invalid_argument_maps_to_502_and_nothing_persisted() throws Exception {
        Config.STUB.set(query -> {
            throw new IllegalArgumentException("simulated INVALID_ARGUMENT from gRPC server");
        });

        mockMvc.perform(post("/api/orders")
                        .with(jwt().jwt(j -> j.subject("user-vertical-003")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateSkuBody()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("INVENTORY_PROTOCOL_ERROR"));

        assertThat(orderCount()).isZero();
        assertThat(outboxCount()).isZero();
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private int orderCount() {
        final Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM order_service.orders", Integer.class);
        return n != null ? n : 0;
    }

    private int outboxCount() {
        final Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM order_service.outbox_events", Integer.class);
        return n != null ? n : 0;
    }

    /** POST body with duplicate SKU-A lines: total SKU-A qty 5 (3+2), SKU-B qty 1. */
    private static String duplicateSkuBody() {
        return """
                {
                  "customerId": "CUST-VERTICAL",
                  "lines": [
                    {"sku": "SKU-A", "quantity": 3},
                    {"sku": "SKU-B", "quantity": 1},
                    {"sku": "SKU-A", "quantity": 2}
                  ]
                }
                """;
    }
}
