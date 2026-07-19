package com.arbitrier.order.application.service;

import com.arbitrier.order.adapter.outbound.InMemoryOrderRepository;
import com.arbitrier.order.adapter.outbound.StubInventoryAvailabilityPort;
import com.arbitrier.order.adapter.outbound.grpc.inventory.InventoryAvailabilityRemoteUnavailableException;
import com.arbitrier.order.adapter.outbound.grpc.inventory.InventoryAvailabilityTimeoutException;
import com.arbitrier.order.application.OrderProblemCode;
import com.arbitrier.order.application.port.inbound.SubmitCorporateBulkOrderCommand;
import com.arbitrier.order.application.port.inbound.SubmitCorporateBulkOrderLineCommand;
import com.arbitrier.order.application.port.inbound.SubmitCorporateBulkOrderResult;
import com.arbitrier.order.application.port.outbound.CustomerAccessPort;
import com.arbitrier.order.application.port.outbound.OrderRepository;
import com.arbitrier.order.domain.model.Order;
import com.arbitrier.order.domain.model.OrderId;
import com.arbitrier.order.domain.model.OrderLine;
import com.arbitrier.order.domain.model.OrderStatus;
import com.arbitrier.platform.error.ApplicationProblemException;
import com.arbitrier.platform.messaging.outbox.mapper.DomainEventToOutboxMapper;
import com.arbitrier.platform.messaging.serialization.JacksonEventSerializer;
import com.arbitrier.platform.messaging.test.InMemoryOutboxRepository;
import com.arbitrier.platform.time.FixedTimeProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class SubmitCorporateBulkOrderServiceTest {

    private InMemoryOrderRepository repository;
    private InMemoryOutboxRepository outboxRepository;
    private DomainEventToOutboxMapper outboxMapper;
    private StubInventoryAvailabilityPort inventoryPort;
    private SubmitCorporateBulkOrderService service;

    /** Allow-all adapter: every user may submit for every customer. */
    private static final CustomerAccessPort ALLOW_ALL = (userId, customerId) -> true;

    /** Deny-all adapter: no user may submit for any customer. */
    private static final CustomerAccessPort DENY_ALL = (userId, customerId) -> false;

    @BeforeEach
    void setUp() {
        repository = new InMemoryOrderRepository();
        outboxRepository = new InMemoryOutboxRepository();
        outboxMapper = new DomainEventToOutboxMapper(
                new JacksonEventSerializer(new ObjectMapper()),
                FixedTimeProvider.of(Instant.parse("2026-01-15T10:00:00Z")));
        inventoryPort = new StubInventoryAvailabilityPort();
        inventoryPort.setUnlimited("SKU-001");
        service = new SubmitCorporateBulkOrderService(
                repository, outboxRepository, outboxMapper, ALLOW_ALL, inventoryPort);
    }

    // ── happy path ─────────────────────────────────────────────────────────────

    @Test
    void happy_path_creates_pending_order() {
        SubmitCorporateBulkOrderCommand command = validCommand();

        SubmitCorporateBulkOrderResult result = service.execute(command);

        assertThat(result.orderId()).isNotBlank();
        assertThat(result.status()).isEqualTo(OrderStatus.PENDING.name());
    }

    @Test
    void repository_save_is_called_once() {
        service.execute(validCommand());

        assertThat(repository.size()).isEqualTo(1);
    }

    @Test
    void saved_order_can_be_retrieved_by_id() {
        SubmitCorporateBulkOrderResult result = service.execute(validCommand());

        assertThat(repository.findAll()).hasSize(1);
        assertThat(repository.findAll().get(0).id().value()).isEqualTo(result.orderId());
    }

    @Test
    void outbox_event_is_saved_once() {
        service.execute(validCommand());

        assertThat(outboxRepository.findAll()).hasSize(1);
    }

    @Test
    void outbox_event_carries_order_created_type() {
        service.execute(validCommand());

        assertThat(outboxRepository.findAll().get(0).eventType())
                .isEqualTo("OrderCreatedDomainEvent");
    }

    @Test
    void outbox_event_carries_correct_aggregate_id() {
        SubmitCorporateBulkOrderResult result = service.execute(validCommand());

        assertThat(outboxRepository.findAll().get(0).aggregateId())
                .isEqualTo(result.orderId());
    }

    @Test
    void outbox_event_carries_order_aggregate_type() {
        service.execute(validCommand());

        assertThat(outboxRepository.findAll().get(0).aggregateType()).isEqualTo("Order");
    }

    // ── customer access control ────────────────────────────────────────────────

    @Test
    void denied_customer_access_throws_application_problem() {
        SubmitCorporateBulkOrderService deniedService =
                new SubmitCorporateBulkOrderService(
                        repository, outboxRepository, outboxMapper, DENY_ALL, inventoryPort);

        assertThatThrownBy(() -> deniedService.execute(validCommand()))
                .isInstanceOf(ApplicationProblemException.class)
                .extracting(ex -> ((ApplicationProblemException) ex).code())
                .isEqualTo(OrderProblemCode.CUSTOMER_ACCESS_DENIED);
    }

    @Test
    void denied_customer_access_maps_to_403() {
        SubmitCorporateBulkOrderService deniedService =
                new SubmitCorporateBulkOrderService(
                        repository, outboxRepository, outboxMapper, DENY_ALL, inventoryPort);

        assertThatThrownBy(() -> deniedService.execute(validCommand()))
                .isInstanceOf(ApplicationProblemException.class)
                .extracting(ex -> ((ApplicationProblemException) ex).code().httpStatus())
                .isEqualTo(403);
    }

    @Test
    void denied_customer_access_does_not_save_order() {
        SubmitCorporateBulkOrderService deniedService =
                new SubmitCorporateBulkOrderService(
                        repository, outboxRepository, outboxMapper, DENY_ALL, inventoryPort);

        assertThatThrownBy(() -> deniedService.execute(validCommand()))
                .isInstanceOf(ApplicationProblemException.class);

        assertThat(repository.size()).isZero();
        assertThat(outboxRepository.findAll()).isEmpty();
    }

    // ── command validation ─────────────────────────────────────────────────────

    @Test
    void missing_customer_id_throws() {
        assertThatNullPointerException()
                .isThrownBy(() -> new SubmitCorporateBulkOrderCommand(null, "USER-001", List.of(validLine())));
    }

    @Test
    void blank_customer_id_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SubmitCorporateBulkOrderCommand("", "USER-001", List.of(validLine())));
    }

    @Test
    void missing_submitted_by_user_id_throws() {
        assertThatNullPointerException()
                .isThrownBy(() -> new SubmitCorporateBulkOrderCommand("CUST-001", null, List.of(validLine())));
    }

    @Test
    void blank_submitted_by_user_id_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SubmitCorporateBulkOrderCommand("CUST-001", "", List.of(validLine())));
    }

    @Test
    void empty_lines_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SubmitCorporateBulkOrderCommand("CUST-001", "USER-001", List.of()));
    }

    @Test
    void invalid_sku_throws() {
        assertThatNullPointerException()
                .isThrownBy(() -> new SubmitCorporateBulkOrderLineCommand(null, 1));
    }

    @Test
    void zero_quantity_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SubmitCorporateBulkOrderLineCommand("SKU-001", 0));
    }

    @Test
    void negative_quantity_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SubmitCorporateBulkOrderLineCommand("SKU-001", -1));
    }

    // ── inventory availability ─────────────────────────────────────────────────

    @Test
    void insufficient_stock_throws_order_items_unavailable() {
        inventoryPort.setAvailable("SKU-001", 5); // requested 10, only 5 available

        assertThatThrownBy(() -> service.execute(validCommand()))
                .isInstanceOf(ApplicationProblemException.class)
                .extracting(ex -> ((ApplicationProblemException) ex).code())
                .isEqualTo(OrderProblemCode.ORDER_ITEMS_UNAVAILABLE);
    }

    @Test
    void insufficient_stock_maps_to_422() {
        inventoryPort.setAvailable("SKU-001", 5);

        assertThatThrownBy(() -> service.execute(validCommand()))
                .isInstanceOf(ApplicationProblemException.class)
                .extracting(ex -> ((ApplicationProblemException) ex).code().httpStatus())
                .isEqualTo(422);
    }

    @Test
    void unknown_sku_zero_availability_prevents_order_creation() {
        // SKU-001 not configured → returns 0
        inventoryPort = new StubInventoryAvailabilityPort();
        service = new SubmitCorporateBulkOrderService(
                repository, outboxRepository, outboxMapper, ALLOW_ALL, inventoryPort);

        assertThatThrownBy(() -> service.execute(validCommand()))
                .isInstanceOf(ApplicationProblemException.class)
                .extracting(ex -> ((ApplicationProblemException) ex).code())
                .isEqualTo(OrderProblemCode.ORDER_ITEMS_UNAVAILABLE);
    }

    @Test
    void insufficient_stock_does_not_save_order_or_event() {
        inventoryPort.setAvailable("SKU-001", 5);

        assertThatThrownBy(() -> service.execute(validCommand()))
                .isInstanceOf(ApplicationProblemException.class);

        assertThat(repository.size()).isZero();
        assertThat(outboxRepository.findAll()).isEmpty();
    }

    @Test
    void all_lines_checked_one_insufficient_prevents_creation() {
        inventoryPort.setUnlimited("SKU-001");
        inventoryPort.setAvailable("SKU-002", 2); // requested 5, only 2 available

        SubmitCorporateBulkOrderCommand multiLineCommand = new SubmitCorporateBulkOrderCommand(
                "CUST-001", "USER-001", List.of(
                        new SubmitCorporateBulkOrderLineCommand("SKU-001", 10),
                        new SubmitCorporateBulkOrderLineCommand("SKU-002", 5)));

        assertThatThrownBy(() -> service.execute(multiLineCommand))
                .isInstanceOf(ApplicationProblemException.class)
                .extracting(ex -> ((ApplicationProblemException) ex).code())
                .isEqualTo(OrderProblemCode.ORDER_ITEMS_UNAVAILABLE);

        assertThat(repository.size()).isZero();
    }

    @Test
    void all_lines_available_creates_order() {
        inventoryPort.setUnlimited("SKU-001", "SKU-002");

        SubmitCorporateBulkOrderCommand multiLineCommand = new SubmitCorporateBulkOrderCommand(
                "CUST-001", "USER-001", List.of(
                        new SubmitCorporateBulkOrderLineCommand("SKU-001", 10),
                        new SubmitCorporateBulkOrderLineCommand("SKU-002", 5)));

        SubmitCorporateBulkOrderResult result = service.execute(multiLineCommand);

        assertThat(result.orderId()).isNotBlank();
        assertThat(repository.size()).isEqualTo(1);
    }

    @Test
    void inventory_timeout_propagates_without_saving_order() {
        InventoryAvailabilityTimeoutException timeout = new InventoryAvailabilityTimeoutException(
                "Inventory timed out", new RuntimeException("cause"));
        service = new SubmitCorporateBulkOrderService(
                repository, outboxRepository, outboxMapper, ALLOW_ALL,
                lines -> { throw timeout; });

        assertThatThrownBy(() -> service.execute(validCommand()))
                .isSameAs(timeout);

        assertThat(repository.size()).isZero();
        assertThat(outboxRepository.findAll()).isEmpty();
    }

    @Test
    void inventory_remote_unavailable_propagates_without_saving_order() {
        InventoryAvailabilityRemoteUnavailableException unavailable =
                new InventoryAvailabilityRemoteUnavailableException(
                        "Inventory service down", new RuntimeException("cause"));
        service = new SubmitCorporateBulkOrderService(
                repository, outboxRepository, outboxMapper, ALLOW_ALL,
                lines -> { throw unavailable; });

        assertThatThrownBy(() -> service.execute(validCommand()))
                .isSameAs(unavailable);

        assertThat(repository.size()).isZero();
        assertThat(outboxRepository.findAll()).isEmpty();
    }

    @Test
    void availability_is_checked_before_order_is_created() {
        // Set availability to 0 — order should never be persisted
        inventoryPort = new StubInventoryAvailabilityPort();
        service = new SubmitCorporateBulkOrderService(
                repository, outboxRepository, outboxMapper, ALLOW_ALL, inventoryPort);

        assertThatThrownBy(() -> service.execute(validCommand()))
                .isInstanceOf(ApplicationProblemException.class);

        assertThat(repository.size()).isZero();
        assertThat(outboxRepository.findAll()).isEmpty();
    }

    // ── ARB-023.3.2: inventory availability verification responsibilities ─────────────────

    @Test
    void repeated_sku_quantities_are_summed() {
        inventoryPort.setAvailable("SKU-001", 15);

        SubmitCorporateBulkOrderCommand command = new SubmitCorporateBulkOrderCommand(
                "CUST-001", "USER-001", List.of(
                        new SubmitCorporateBulkOrderLineCommand("SKU-001", 10),
                        new SubmitCorporateBulkOrderLineCommand("SKU-001", 5)));

        SubmitCorporateBulkOrderResult result = service.execute(command);

        assertThat(result.orderId()).isNotBlank();
        assertThat(repository.size()).isEqualTo(1);
    }

    @Test
    void repeated_sku_with_insufficient_total_throws_order_items_unavailable() {
        inventoryPort.setAvailable("SKU-001", 10);

        SubmitCorporateBulkOrderCommand command = new SubmitCorporateBulkOrderCommand(
                "CUST-001", "USER-001", List.of(
                        new SubmitCorporateBulkOrderLineCommand("SKU-001", 10),
                        new SubmitCorporateBulkOrderLineCommand("SKU-001", 5)));

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(ApplicationProblemException.class)
                .extracting(ex -> ((ApplicationProblemException) ex).code())
                .isEqualTo(OrderProblemCode.ORDER_ITEMS_UNAVAILABLE);

        assertThat(repository.size()).isZero();
    }

    @Test
    void persisted_order_uses_normalized_quantities() {
        inventoryPort.setUnlimited("SKU-001");

        SubmitCorporateBulkOrderCommand command = new SubmitCorporateBulkOrderCommand(
                "CUST-001", "USER-001", List.of(
                        new SubmitCorporateBulkOrderLineCommand("SKU-001", 10),
                        new SubmitCorporateBulkOrderLineCommand("SKU-001", 5)));

        service.execute(command);

        final Order savedOrder = repository.findAll().get(0);
        final OrderLine line = savedOrder.lines().get(0);
        assertThat(line.sku().value()).isEqualTo("SKU-001");
        assertThat(line.quantity().value()).isEqualTo(15);
    }

    @Test
    void negative_quantity_rejected_by_command() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SubmitCorporateBulkOrderLineCommand("SKU-001", -1));
    }

    @Test
    void zero_quantity_rejected_by_command() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SubmitCorporateBulkOrderLineCommand("SKU-001", 0));
    }

    // ── ARB-023.5: saga start acceptance semantics ────────────────────────────

    @Test
    void returned_status_equals_persisted_order_status() {
        SubmitCorporateBulkOrderResult result = service.execute(validCommand());

        Order persistedOrder = repository.findAll().get(0);
        assertThat(result.status()).isEqualTo(persistedOrder.status().name());
    }

    @Test
    void saga_start_event_payload_contains_normalized_sku_quantities() throws Exception {
        inventoryPort.setUnlimited("SKU-001");

        SubmitCorporateBulkOrderCommand command = new SubmitCorporateBulkOrderCommand(
                "CUST-001", "USER-001", List.of(
                        new SubmitCorporateBulkOrderLineCommand("SKU-001", 10),
                        new SubmitCorporateBulkOrderLineCommand("SKU-001", 5)));

        service.execute(command);

        String payload = outboxRepository.findAll().get(0).payload();
        JsonNode root = new ObjectMapper().readTree(payload);
        JsonNode lines = root.path("lines");

        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).path("sku").path("value").asText()).isEqualTo("SKU-001");
        assertThat(lines.get(0).path("quantity").path("value").asInt()).isEqualTo(15);
    }

    @Test
    void order_persistence_failure_prevents_outbox_event() {
        SubmitCorporateBulkOrderService failingService = new SubmitCorporateBulkOrderService(
                new OrderRepository() {
                    @Override public void save(Order o) { throw new RuntimeException("simulated DB failure"); }
                    @Override public Optional<Order> findById(OrderId id) { return Optional.empty(); }
                },
                outboxRepository,
                outboxMapper,
                ALLOW_ALL,
                inventoryPort);

        assertThatThrownBy(() -> failingService.execute(validCommand()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("simulated DB failure");

        assertThat(outboxRepository.findAll()).isEmpty();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static SubmitCorporateBulkOrderCommand validCommand() {
        return new SubmitCorporateBulkOrderCommand("CUST-001", "USER-001", List.of(validLine()));
    }

    private static SubmitCorporateBulkOrderLineCommand validLine() {
        return new SubmitCorporateBulkOrderLineCommand("SKU-001", 10);
    }
}
