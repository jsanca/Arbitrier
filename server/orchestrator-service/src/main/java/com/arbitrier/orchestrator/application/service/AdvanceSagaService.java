package com.arbitrier.orchestrator.application.service;

import com.arbitrier.orchestrator.application.port.inbound.AdvanceSagaCommand;
import com.arbitrier.orchestrator.application.port.inbound.AdvanceSagaResult;
import com.arbitrier.orchestrator.application.port.inbound.AdvanceSagaUseCase;
import com.arbitrier.orchestrator.application.port.outbound.SagaEventPublisher;
import com.arbitrier.orchestrator.application.port.outbound.SagaRepository;
import com.arbitrier.orchestrator.domain.event.SagaAdvancedDomainEvent;
import com.arbitrier.orchestrator.domain.model.Saga;
import com.arbitrier.orchestrator.domain.model.SagaId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use-case implementation: advance a saga to a new processing step.
 *
 * <p>Loads the saga, calls {@link Saga#advance(com.arbitrier.orchestrator.domain.model.SagaStep)},
 * persists the updated aggregate, and publishes {@link SagaAdvancedDomainEvent}.
 *
 * <p>Business step-sequencing logic (which step follows which) belongs to ARB-015.
 * This service is a general-purpose step-transition mechanism.
 *
 * <p>Layer: application/service
 * <p>Module: orchestrator-service
 */
public class AdvanceSagaService implements AdvanceSagaUseCase {

    private static final Logger log = LoggerFactory.getLogger(AdvanceSagaService.class);

    private final SagaRepository repository;
    private final SagaEventPublisher eventPublisher;

    public AdvanceSagaService(SagaRepository repository, SagaEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public AdvanceSagaResult advance(final AdvanceSagaCommand command) {
        final SagaId sagaId = SagaId.of(command.sagaId());

        final Saga saga = repository.findById(sagaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No saga found with id: " + sagaId));

        final Saga advanced = saga.advance(command.nextStep());

        repository.save(advanced);
        eventPublisher.publishAdvanced(
                new SagaAdvancedDomainEvent(sagaId, saga.orderId(), advanced.currentStep()));

        log.info("Saga advanced sagaId={} orderId={} step={} status={}",
                sagaId, saga.orderId(), advanced.currentStep(), advanced.status());

        return new AdvanceSagaResult(sagaId, advanced.currentStep());
    }
}
