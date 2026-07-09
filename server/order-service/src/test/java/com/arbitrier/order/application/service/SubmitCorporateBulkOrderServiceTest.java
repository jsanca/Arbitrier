package com.arbitrier.order.application.service;

import com.arbitrier.order.adapter.outbound.InMemoryOrderRepository;
import com.arbitrier.order.adapter.outbound.RecordingOrderEventPublisher;
import com.arbitrier.order.application.OrderProblemCode;
import com.arbitrier.order.application.port.inbound.SubmitCorporateBulkOrderCommand;
import com.arbitrier.order.application.port.inbound.SubmitCorporateBulkOrderLineCommand;
import com.arbitrier.order.application.port.inbound.SubmitCorporateBulkOrderResult;
import com.arbitrier.order.application.port.outbound.CustomerAccessPort;
import com.arbitrier.order.domain.model.OrderStatus;
import com.arbitrier.platform.error.ApplicationProblemException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class SubmitCorporateBulkOrderServiceTest {

    private InMemoryOrderRepository repository;
    private RecordingOrderEventPublisher publisher;
    private SubmitCorporateBulkOrderService service;

    /** Allow-all adapter: every user may submit for every customer. */
    private static final CustomerAccessPort ALLOW_ALL = (userId, customerId) -> true;

    /** Deny-all adapter: no user may submit for any customer. */
    private static final CustomerAccessPort DENY_ALL = (userId, customerId) -> false;

    @BeforeEach
    void setUp() {
        repository = new InMemoryOrderRepository();
        publisher  = new RecordingOrderEventPublisher();
        service    = new SubmitCorporateBulkOrderService(repository, publisher, ALLOW_ALL);
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
    void event_publisher_is_called_once() {
        service.execute(validCommand());

        assertThat(publisher.hasEvents()).isTrue();
        assertThat(publisher.events()).hasSize(1);
    }

    @Test
    void published_event_carries_correct_order_id() {
        SubmitCorporateBulkOrderResult result = service.execute(validCommand());

        assertThat(publisher.events().get(0).orderId().value()).isEqualTo(result.orderId());
    }

    @Test
    void published_event_carries_correct_customer_and_user() {
        service.execute(validCommand());

        var event = publisher.events().get(0);
        assertThat(event.customerId().value()).isEqualTo("CUST-001");
        assertThat(event.submittedBy().value()).isEqualTo("USER-001");
    }

    // ── customer access control ────────────────────────────────────────────────

    @Test
    void denied_customer_access_throws_application_problem() {
        SubmitCorporateBulkOrderService deniedService =
                new SubmitCorporateBulkOrderService(repository, publisher, DENY_ALL);

        assertThatThrownBy(() -> deniedService.execute(validCommand()))
                .isInstanceOf(ApplicationProblemException.class)
                .extracting(ex -> ((ApplicationProblemException) ex).code())
                .isEqualTo(OrderProblemCode.CUSTOMER_ACCESS_DENIED);
    }

    @Test
    void denied_customer_access_maps_to_403() {
        SubmitCorporateBulkOrderService deniedService =
                new SubmitCorporateBulkOrderService(repository, publisher, DENY_ALL);

        assertThatThrownBy(() -> deniedService.execute(validCommand()))
                .isInstanceOf(ApplicationProblemException.class)
                .extracting(ex -> ((ApplicationProblemException) ex).code().httpStatus())
                .isEqualTo(403);
    }

    @Test
    void denied_customer_access_does_not_save_order() {
        SubmitCorporateBulkOrderService deniedService =
                new SubmitCorporateBulkOrderService(repository, publisher, DENY_ALL);

        assertThatThrownBy(() -> deniedService.execute(validCommand()))
                .isInstanceOf(ApplicationProblemException.class);

        assertThat(repository.size()).isZero();
        assertThat(publisher.hasEvents()).isFalse();
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

    // ── helpers ───────────────────────────────────────────────────────────────

    private static SubmitCorporateBulkOrderCommand validCommand() {
        return new SubmitCorporateBulkOrderCommand("CUST-001", "USER-001", List.of(validLine()));
    }

    private static SubmitCorporateBulkOrderLineCommand validLine() {
        return new SubmitCorporateBulkOrderLineCommand("SKU-001", 10);
    }
}
