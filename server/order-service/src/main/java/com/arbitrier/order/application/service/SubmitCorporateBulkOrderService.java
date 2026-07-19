package com.arbitrier.order.application.service;

import com.arbitrier.order.application.OrderProblemCode;
import com.arbitrier.order.application.port.inbound.SubmitCorporateBulkOrderCommand;
import com.arbitrier.order.application.port.inbound.SubmitCorporateBulkOrderLineCommand;
import com.arbitrier.order.application.port.inbound.SubmitCorporateBulkOrderResult;
import com.arbitrier.order.application.port.inbound.SubmitCorporateBulkOrderUseCase;
import com.arbitrier.order.application.port.outbound.AvailabilityLineQuery;
import com.arbitrier.order.application.port.outbound.AvailabilityLineResponse;
import com.arbitrier.order.application.port.outbound.CustomerAccessPort;
import com.arbitrier.order.application.port.outbound.InventoryAvailabilityPort;
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
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.outbox.mapper.DomainEventToOutboxMapper;
import com.arbitrier.platform.validation.Require;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application service that handles {@link SubmitCorporateBulkOrderUseCase}.
 *
 * <p>Validates the command, checks customer access via {@link CustomerAccessPort},
 * constructs an {@link Order} in {@code PENDING} state, persists it through
 * {@link OrderRepository}, and writes an {@link OrderCreatedDomainEvent} to the
 * transactional outbox for downstream publication.
 *
 * <p>Layer: application/service
 * <p>Module: order-service
 */
public class SubmitCorporateBulkOrderService implements SubmitCorporateBulkOrderUseCase {

    private static final Logger log = LoggerFactory.getLogger(SubmitCorporateBulkOrderService.class);

    private static final String AGGREGATE_TYPE = "Order";

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final DomainEventToOutboxMapper outboxMapper;
    private final CustomerAccessPort customerAccessPort;
    private final InventoryAvailabilityPort inventoryAvailabilityPort;

    public SubmitCorporateBulkOrderService(OrderRepository orderRepository,
                                           OutboxRepository outboxRepository,
                                           DomainEventToOutboxMapper outboxMapper,
                                           CustomerAccessPort customerAccessPort,
                                           InventoryAvailabilityPort inventoryAvailabilityPort) {
        this.orderRepository = Require.notNull(orderRepository, "orderRepository");
        this.outboxRepository = Require.notNull(outboxRepository, "outboxRepository");
        this.outboxMapper = Require.notNull(outboxMapper, "outboxMapper");
        this.customerAccessPort = Require.notNull(customerAccessPort, "customerAccessPort");
        this.inventoryAvailabilityPort = Require.notNull(inventoryAvailabilityPort, "inventoryAvailabilityPort");
    }

    @Override
    @Transactional
    public SubmitCorporateBulkOrderResult execute(final SubmitCorporateBulkOrderCommand command) {

        Require.notNull(command, "command");

        if (!customerAccessPort.canSubmitOrder(command.submittedByUserId(), command.customerId())) {
            log.warn("Customer access denied: customerId={}", command.customerId());
            throw new ApplicationProblemException(
                    OrderProblemCode.CUSTOMER_ACCESS_DENIED,
                    OrderProblemCode.CUSTOMER_ACCESS_DENIED.description());
        }

        final LinkedHashMap<String, Integer> requestedBySku = aggregateRequestedQuantitiesBySku(command);

        this.verifyInventoryAvailability(requestedBySku);

        final OrderId orderId = OrderId.of(UUID.randomUUID().toString());

        final List<OrderLine> domainLines = requestedBySku.entrySet().stream()
                .map(e -> new OrderLine(Sku.of(e.getKey()), Quantity.of(e.getValue())))
                .toList();

        final Order order = Order.create(
                orderId,
                CustomerId.of(command.customerId()),
                UserId.of(command.submittedByUserId()),
                domainLines);

        this.orderRepository.save(order);

        // OPEN QUESTION: correlationId is not yet part of the command — mark for observability wiring.
        log.info("Order created: orderId={}, customerId={}, lines={}",
                orderId, command.customerId(), domainLines.size());

        final OrderCreatedDomainEvent event = new OrderCreatedDomainEvent(
                orderId,
                order.customerId(),
                order.submittedBy(),
                order.lines());

        this.outboxRepository.save(this.outboxMapper.map(event, orderId.value(), AGGREGATE_TYPE));

        return new SubmitCorporateBulkOrderResult(orderId.value(), order.status().name());
    }

    private void verifyInventoryAvailability(final Map<String, Integer> requestedBySku) {

        final List<AvailabilityLineQuery> queries = requestedBySku.entrySet().stream()
                .map(e -> new AvailabilityLineQuery(e.getKey(), e.getValue()))
                .toList();

        final List<AvailabilityLineResponse> responses = this.inventoryAvailabilityPort.checkAvailability(queries);

        final boolean allAvailable = responses.stream()
                .allMatch(r -> r.availableQuantity() >= requestedBySku.getOrDefault(r.sku(), 0));

        if (!allAvailable) {
            log.info("Order rejected: insufficient inventory. requestedSkus={}", requestedBySku.keySet());
            throw new ApplicationProblemException(
                    OrderProblemCode.ORDER_ITEMS_UNAVAILABLE,
                    OrderProblemCode.ORDER_ITEMS_UNAVAILABLE.description());
        }
    }

    private LinkedHashMap<String, Integer> aggregateRequestedQuantitiesBySku(
            final SubmitCorporateBulkOrderCommand command) {
        return command.lines().stream()
                .collect(Collectors.toMap(
                        SubmitCorporateBulkOrderLineCommand::sku,
                        SubmitCorporateBulkOrderLineCommand::quantity,
                        Integer::sum,
                        LinkedHashMap::new));
    }
}
