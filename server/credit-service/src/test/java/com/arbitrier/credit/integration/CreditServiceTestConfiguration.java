package com.arbitrier.credit.integration;

import com.arbitrier.credit.adapter.outbound.ConfigurableCreditLimitPort;
import com.arbitrier.credit.adapter.outbound.InMemoryCreditReservationRepository;
import com.arbitrier.credit.adapter.outbound.RecordingCreditReservationEventPublisher;
import com.arbitrier.credit.application.port.outbound.CreditLimitPort;
import com.arbitrier.credit.application.port.outbound.CreditReservationEventPublisher;
import com.arbitrier.credit.application.port.outbound.CreditReservationRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration: wires in-memory adapters for Spring context load tests.
 *
 * <p>All three outbound port beans are marked {@code @Primary} so they take precedence over
 * any production adapter that might also be present in the context.
 *
 * <p>Layer: integration test
 * <p>Module: credit-service
 */
@TestConfiguration
public class CreditServiceTestConfiguration {

    @Bean
    @Primary
    public CreditLimitPort creditLimitPort() {
        return new ConfigurableCreditLimitPort();
    }

    @Bean
    @Primary
    public CreditReservationRepository creditReservationRepository() {
        return new InMemoryCreditReservationRepository();
    }

    @Bean
    @Primary
    public CreditReservationEventPublisher creditReservationEventPublisher() {
        return new RecordingCreditReservationEventPublisher();
    }
}
