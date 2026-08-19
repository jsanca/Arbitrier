package com.arbitrier.platform.correlation;

import com.arbitrier.platform.logging.StructuredLogFields;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class MdcCorrelationContextBridgeTest {

    @AfterEach
    void cleanup() {
        MDC.clear();
    }

    @Test
    void bind_sets_correlationId_in_mdc() {
        MdcCorrelationContextBridge.bind(new DefaultCorrelationContext("cid-1", "rid-1"));

        assertThat(MDC.get(StructuredLogFields.CORRELATION_ID)).isEqualTo("cid-1");
    }

    @Test
    void bind_sets_requestId_in_mdc_when_present() {
        MdcCorrelationContextBridge.bind(new DefaultCorrelationContext("cid-1", "rid-1"));

        assertThat(MDC.get(StructuredLogFields.REQUEST_ID)).isEqualTo("rid-1");
    }

    @Test
    void bind_does_not_set_requestId_when_null() {
        MdcCorrelationContextBridge.bind(new DefaultCorrelationContext("cid-1", null));

        assertThat(MDC.get(StructuredLogFields.REQUEST_ID)).isNull();
    }

    @Test
    void unbind_removes_correlationId() {
        MdcCorrelationContextBridge.bind(new DefaultCorrelationContext("cid-1", "rid-1"));

        MdcCorrelationContextBridge.unbind();

        assertThat(MDC.get(StructuredLogFields.CORRELATION_ID)).isNull();
    }

    @Test
    void unbind_removes_requestId() {
        MdcCorrelationContextBridge.bind(new DefaultCorrelationContext("cid-1", "rid-1"));

        MdcCorrelationContextBridge.unbind();

        assertThat(MDC.get(StructuredLogFields.REQUEST_ID)).isNull();
    }

    @Test
    void unbind_is_safe_when_nothing_bound() {
        MdcCorrelationContextBridge.unbind();

        assertThat(MDC.get(StructuredLogFields.CORRELATION_ID)).isNull();
        assertThat(MDC.get(StructuredLogFields.REQUEST_ID)).isNull();
    }
}
