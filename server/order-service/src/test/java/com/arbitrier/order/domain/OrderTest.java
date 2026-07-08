package com.arbitrier.order.domain;

import com.arbitrier.order.domain.model.CancellationReason;
import com.arbitrier.order.domain.model.CustomerId;
import com.arbitrier.order.domain.model.Order;
import com.arbitrier.order.domain.model.OrderId;
import com.arbitrier.order.domain.model.OrderLine;
import com.arbitrier.order.domain.model.OrderStatus;
import com.arbitrier.order.domain.model.Quantity;
import com.arbitrier.order.domain.model.Sku;
import com.arbitrier.order.domain.model.UserId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Unit tests for the {@link Order} aggregate root.
 */
class OrderTest {

    private static final OrderId ORDER_ID = OrderId.of("order-001");
    private static final CustomerId CUSTOMER_ID = CustomerId.of("cust-001");
    private static final UserId USER_ID = UserId.of("user-001");
    private static final List<OrderLine> LINES = List.of(
            new OrderLine(Sku.of("SKU-A"), Quantity.of(5))
    );

    @Test
    void valid_order_creation_sets_all_fields_and_status_is_pending() {
        Order order = Order.create(ORDER_ID, CUSTOMER_ID, USER_ID, LINES);

        assertThat(order.id()).isEqualTo(ORDER_ID);
        assertThat(order.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(order.submittedBy()).isEqualTo(USER_ID);
        assertThat(order.lines()).hasSize(1);
        assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.cancellationReason()).isNull();
    }

    @Test
    void order_rejects_empty_lines() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Order.create(ORDER_ID, CUSTOMER_ID, USER_ID, List.of()))
                .withMessageContaining("Order.lines");
    }

    @Test
    void quantity_rejects_zero() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Quantity.of(0))
                .withMessageContaining("Quantity.value must be positive");
    }

    @Test
    void quantity_rejects_negative() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Quantity.of(-1))
                .withMessageContaining("Quantity.value must be positive");
    }

    @Test
    void cancel_null_reason_throws_null_pointer_exception() {
        Order order = Order.create(ORDER_ID, CUSTOMER_ID, USER_ID, LINES);

        assertThatNullPointerException()
                .isThrownBy(() -> order.cancel(null));
    }

    @Test
    void confirm_transitions_pending_to_confirmed() {
        Order order = Order.create(ORDER_ID, CUSTOMER_ID, USER_ID, LINES);

        Order confirmed = order.confirm();

        assertThat(confirmed.status()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void confirm_from_non_pending_throws() {
        Order order = Order.create(ORDER_ID, CUSTOMER_ID, USER_ID, LINES)
                .confirm();

        assertThatIllegalArgumentException()
                .isThrownBy(order::confirm)
                .withMessageContaining("PENDING");
    }

    @Test
    void confirm_partially_transitions_awaiting_decision_to_partially_confirmed() {
        Order order = Order.create(ORDER_ID, CUSTOMER_ID, USER_ID, LINES)
                .awaitCustomerDecision();

        Order partial = order.confirmPartially();

        assertThat(partial.status()).isEqualTo(OrderStatus.PARTIALLY_CONFIRMED);
    }

    @Test
    void confirm_partially_from_pending_throws() {
        Order order = Order.create(ORDER_ID, CUSTOMER_ID, USER_ID, LINES);

        assertThatIllegalArgumentException()
                .isThrownBy(order::confirmPartially)
                .withMessageContaining("AWAITING_CUSTOMER_DECISION");
    }

    @Test
    void await_customer_decision_transitions_pending_to_awaiting() {
        Order order = Order.create(ORDER_ID, CUSTOMER_ID, USER_ID, LINES);

        Order waiting = order.awaitCustomerDecision();

        assertThat(waiting.status()).isEqualTo(OrderStatus.AWAITING_CUSTOMER_DECISION);
    }

    @Test
    void cancel_from_terminal_state_throws() {
        Order terminal = Order.create(ORDER_ID, CUSTOMER_ID, USER_ID, LINES).confirm();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> terminal.cancel(CancellationReason.CUSTOMER_CANCELLED))
                .withMessageContaining("non-terminal");
    }

    @Test
    void cancellation_reason_is_set_on_cancelled_order() {
        Order order = Order.create(ORDER_ID, CUSTOMER_ID, USER_ID, LINES);

        Order cancelled = order.cancel(CancellationReason.INSUFFICIENT_CREDIT);

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelled.cancellationReason()).isEqualTo(CancellationReason.INSUFFICIENT_CREDIT);
    }
}
