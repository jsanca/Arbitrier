package com.arbitrier.orchestrator.application.port.outbound;

import com.arbitrier.orchestrator.domain.event.CreditTimedOutDomainEvent;
import com.arbitrier.orchestrator.domain.event.InventoryTimedOutDomainEvent;
import com.arbitrier.orchestrator.domain.event.SagaAdvancedDomainEvent;
import com.arbitrier.orchestrator.domain.event.SagaCancelledDomainEvent;
import com.arbitrier.orchestrator.domain.event.SagaCompensatedDomainEvent;
import com.arbitrier.orchestrator.domain.event.SagaCompensationFailedDomainEvent;
import com.arbitrier.orchestrator.domain.event.SagaCompletedDomainEvent;
import com.arbitrier.orchestrator.domain.event.SagaStartedDomainEvent;

/**
 * Outbound port: publishes saga lifecycle domain events.
 *
 * <p>Layer: application/port/outbound
 * <p>Module: orchestrator-service
 */
public interface SagaEventPublisher {

    /** Publishes a {@link SagaStartedDomainEvent}. */
    void publishStarted(SagaStartedDomainEvent event);

    /** Publishes a {@link SagaAdvancedDomainEvent}. */
    void publishAdvanced(SagaAdvancedDomainEvent event);

    /** Publishes a {@link SagaCompensatedDomainEvent}. */
    void publishCompensated(SagaCompensatedDomainEvent event);

    /** Publishes a {@link SagaCompletedDomainEvent}. */
    void publishCompleted(SagaCompletedDomainEvent event);

    /** Publishes a {@link SagaCancelledDomainEvent}. */
    void publishCancelled(SagaCancelledDomainEvent event);

    /** Publishes a {@link SagaCompensationFailedDomainEvent}. */
    void publishCompensationFailed(SagaCompensationFailedDomainEvent event);

    /** Publishes an {@link InventoryTimedOutDomainEvent}. */
    void publishInventoryTimedOut(InventoryTimedOutDomainEvent event);

    /** Publishes a {@link CreditTimedOutDomainEvent}. */
    void publishCreditTimedOut(CreditTimedOutDomainEvent event);
}
