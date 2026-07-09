package com.arbitrier.orchestrator.adapter.outbound;

import com.arbitrier.orchestrator.application.port.outbound.SagaEventPublisher;
import com.arbitrier.orchestrator.domain.event.SagaAdvancedDomainEvent;
import com.arbitrier.orchestrator.domain.event.SagaCompensatedDomainEvent;
import com.arbitrier.orchestrator.domain.event.SagaCompletedDomainEvent;
import com.arbitrier.orchestrator.domain.event.SagaStartedDomainEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Test adapter: records published saga events for assertion in tests.
 *
 * <p>Layer: adapter/outbound (test)
 * <p>Module: orchestrator-service
 */
public class RecordingSagaEventPublisher implements SagaEventPublisher {

    private final List<SagaStartedDomainEvent> startedEvents = new ArrayList<>();
    private final List<SagaAdvancedDomainEvent> advancedEvents = new ArrayList<>();
    private final List<SagaCompensatedDomainEvent> compensatedEvents = new ArrayList<>();
    private final List<SagaCompletedDomainEvent> completedEvents = new ArrayList<>();

    @Override
    public void publishStarted(final SagaStartedDomainEvent event) {
        startedEvents.add(event);
    }

    @Override
    public void publishAdvanced(final SagaAdvancedDomainEvent event) {
        advancedEvents.add(event);
    }

    @Override
    public void publishCompensated(final SagaCompensatedDomainEvent event) {
        compensatedEvents.add(event);
    }

    @Override
    public void publishCompleted(final SagaCompletedDomainEvent event) {
        completedEvents.add(event);
    }

    public List<SagaStartedDomainEvent> startedEvents() {
        return List.copyOf(startedEvents);
    }

    public List<SagaAdvancedDomainEvent> advancedEvents() {
        return List.copyOf(advancedEvents);
    }

    public List<SagaCompensatedDomainEvent> compensatedEvents() {
        return List.copyOf(compensatedEvents);
    }

    public List<SagaCompletedDomainEvent> completedEvents() {
        return List.copyOf(completedEvents);
    }

    public int totalEventCount() {
        return startedEvents.size() + advancedEvents.size()
                + compensatedEvents.size() + completedEvents.size();
    }
}
