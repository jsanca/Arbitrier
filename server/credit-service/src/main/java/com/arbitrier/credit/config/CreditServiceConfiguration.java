package com.arbitrier.credit.config;

import com.arbitrier.credit.application.port.inbound.ReleaseCreditUseCase;
import com.arbitrier.credit.application.port.inbound.ReserveCreditUseCase;
import com.arbitrier.credit.application.port.outbound.CreditLimitPort;
import com.arbitrier.credit.application.port.outbound.CreditReservationRepository;
import com.arbitrier.credit.application.service.ReleaseCreditService;
import com.arbitrier.credit.application.service.ReserveCreditService;
import com.arbitrier.platform.messaging.outbox.OutboxRepository;
import com.arbitrier.platform.messaging.outbox.mapper.DomainEventToOutboxMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring wiring: binds application services to their outbound port implementations.
 *
 * <p>Layer: config
 * <p>Module: credit-service
 */
@Configuration
public class CreditServiceConfiguration {

    @Bean
    public ReserveCreditUseCase reserveCreditService(
            CreditLimitPort creditLimitPort,
            CreditReservationRepository creditReservationRepository,
            OutboxRepository outboxRepository,
            DomainEventToOutboxMapper outboxMapper) {
        return new ReserveCreditService(
                creditLimitPort, creditReservationRepository, outboxRepository, outboxMapper);
    }

    @Bean
    public ReleaseCreditUseCase releaseCreditService(
            CreditReservationRepository creditReservationRepository,
            OutboxRepository outboxRepository,
            DomainEventToOutboxMapper outboxMapper) {
        return new ReleaseCreditService(creditReservationRepository, outboxRepository, outboxMapper);
    }
}
