package com.arbitrier.orchestrator.application.port.outbound;

/**
 * Outbound port: issues a release-credit command to the credit-service during compensation.
 *
 * <p>OPEN QUESTION (ARB-017): No ARB-016 flow currently issues this command. It is defined
 * here for future compensation paths where an approved credit reservation must be released.
 *
 * <p>Layer: application/port/outbound
 * <p>Module: orchestrator-service
 */
public interface ReleaseCreditCommandPublisher {

    /** Publishes a {@link ReleaseCreditSagaCommand}. */
    void publishReleaseCredit(ReleaseCreditSagaCommand command);
}
