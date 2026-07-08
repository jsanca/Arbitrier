package com.arbitrier.orchestrator.domain.model;

import com.arbitrier.platform.validation.Require;

/**
 * Unique identifier for a UC-01 saga instance.
 *
 * <p>Layer: domain/model
 * <p>Module: orchestrator-service
 */
public record SagaId(String value) {
    public SagaId {
        Require.notBlank(value, "SagaId.value");
    }

    /** Creates a {@code SagaId} from the given string. */
    public static SagaId of(String value) {
        return new SagaId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
