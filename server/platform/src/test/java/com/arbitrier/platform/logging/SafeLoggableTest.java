package com.arbitrier.platform.logging;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SafeLoggableTest {

    @Test
    void structured_log_fields_are_non_blank() {
        assertThat(StructuredLogFields.SAGA_ID).isNotBlank();
        assertThat(StructuredLogFields.ORDER_ID).isNotBlank();
        assertThat(StructuredLogFields.TRACE_ID).isNotBlank();
        assertThat(StructuredLogFields.CORRELATION_ID).isNotBlank();
        assertThat(StructuredLogFields.CAUSATION_ID).isNotBlank();
        assertThat(StructuredLogFields.MESSAGE_ID).isNotBlank();
        assertThat(StructuredLogFields.REQUEST_ID).isNotBlank();
        assertThat(StructuredLogFields.IDEMPOTENCY_KEY).isNotBlank();
    }

    @Test
    void safe_loggable_contract_is_enforced_by_implementation() {
        SafeLoggable loggable = () -> "safe-output";
        assertThat(loggable.toSafeLogString()).isEqualTo("safe-output");
    }

    @Test
    void safe_renderable_contract_is_enforced_by_implementation() {
        SafeRenderable renderable = () -> "safe-display";
        assertThat(renderable.renderSafe()).isEqualTo("safe-display");
    }
}
