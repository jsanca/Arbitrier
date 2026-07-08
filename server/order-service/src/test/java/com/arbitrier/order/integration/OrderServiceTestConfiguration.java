package com.arbitrier.order.integration;

import com.arbitrier.order.adapter.outbound.InMemoryOrderRepository;
import com.arbitrier.order.adapter.outbound.RecordingOrderEventPublisher;
import com.arbitrier.order.application.port.outbound.OrderEventPublisher;
import com.arbitrier.order.application.port.outbound.OrderRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class OrderServiceTestConfiguration {
    @Bean OrderRepository orderRepository() { return new InMemoryOrderRepository(); }
    @Bean OrderEventPublisher orderEventPublisher() { return new RecordingOrderEventPublisher(); }
}
