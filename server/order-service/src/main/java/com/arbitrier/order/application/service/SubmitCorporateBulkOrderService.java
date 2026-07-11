package com.arbitrier.order.application.service;

import com.arbitrier.order.application.OrderProblemCode;
import com.arbitrier.order.application.port.inbound.SubmitCorporateBulkOrderCommand;
import com.arbitrier.order.application.port.inbound.SubmitCorporateBulkOrderResult;
import com.arbitrier.order.application.port.inbound.SubmitCorporateBulkOrderUseCase;
import com.arbitrier.order.application.port.outbound.CustomerAccessPort;
import com.arbitrier.order.application.port.outbound.OrderEventPublisher;
import com.arbitrier.order.application.port.outbound.OrderRepository;
import com.arbitrier.order.domain.event.OrderCreatedDomainEvent;
import com.arbitrier.order.domain.model.CustomerId;
import com.arbitrier.order.domain.model.Order;
import com.arbitrier.order.domain.model.OrderId;
import com.arbitrier.order.domain.model.OrderLine;
import com.arbitrier.order.domain.model.Quantity;
import com.arbitrier.order.domain.model.Sku;
import com.arbitrier.order.domain.model.UserId;
import com.arbitrier.platform.error.ApplicationProblemException;
import com.arbitrier.platform.validation.Require;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Application service that handles {@link SubmitCorporateBulkOrderUseCase}.
 *
 * <p>Validates the command, checks customer access via {@link CustomerAccessPort},
 * constructs an {@link Order} in {@code PENDING} state, persists it through
 * {@link OrderRepository}, and publishes an {@link OrderCreatedDomainEvent} through
 * {@link OrderEventPublisher}.
 *
 * <p>Layer: application/service
 * <p>Module: order-service
 */
public class SubmitCorporateBulkOrderService implements SubmitCorporateBulkOrderUseCase {

    private static final Logger log = LoggerFactory.getLogger(SubmitCorporateBulkOrderService.class);

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;
    private final CustomerAccessPort customerAccessPort;

    public SubmitCorporateBulkOrderService(OrderRepository orderRepository,
                                           OrderEventPublisher eventPublisher,
                                           CustomerAccessPort customerAccessPort) {
        this.orderRepository = Require.notNull(orderRepository, "orderRepository");
        this.eventPublisher = Require.notNull(eventPublisher, "eventPublisher");
        this.customerAccessPort = Require.notNull(customerAccessPort, "customerAccessPort");
    }

    @Override
    @Transactional
    public SubmitCorporateBulkOrderResult execute(SubmitCorporateBulkOrderCommand command) {
        Require.notNull(command, "command");

        if (!customerAccessPort.canSubmitOrder(command.submittedByUserId(), command.customerId())) {
            log.warn("Customer access denied: customerId={}", command.customerId());
            throw new ApplicationProblemException(
                    OrderProblemCode.CUSTOMER_ACCESS_DENIED,
                    OrderProblemCode.CUSTOMER_ACCESS_DENIED.description());
        }

        OrderId orderId = OrderId.of(UUID.randomUUID().toString());

        List<OrderLine> domainLines = command.lines().stream()
                .map(l -> new OrderLine(Sku.of(l.sku()), Quantity.of(l.quantity())))
                .toList();

        Order order = Order.create(
                orderId,
                CustomerId.of(command.customerId()),
                UserId.of(command.submittedByUserId()),
                domainLines);

        orderRepository.save(order);

        // OPEN QUESTION: correlationId is not yet part of the command — mark for observability wiring.
        log.info("Order created: orderId={}, customerId={}, lines={}",
                orderId, command.customerId(), domainLines.size());

        OrderCreatedDomainEvent event = new OrderCreatedDomainEvent(
                orderId,
                order.customerId(),
                order.submittedBy(),
                order.lines());

        eventPublisher.publish(event);

        return new SubmitCorporateBulkOrderResult(orderId.value(), order.status().name());
    }
}
