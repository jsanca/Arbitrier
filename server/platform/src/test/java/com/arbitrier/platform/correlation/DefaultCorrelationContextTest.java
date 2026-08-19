package com.arbitrier.platform.correlation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class DefaultCorrelationContextTest {

    @Test
    void rejects_null_correlationId() {
        assertThatNullPointerException()
                .isThrownBy(() -> new DefaultCorrelationContext(null, "req-1"));
    }

    @Test
    void rejects_blank_correlationId() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DefaultCorrelationContext("  ", "req-1"));
    }

    @Test
    void accepts_null_requestId() {
        var ctx = new DefaultCorrelationContext("cid-1", null);

        assertThat(ctx.correlationId()).isEqualTo("cid-1");
        assertThat(ctx.requestId()).isNull();
    }

    @Test
    void stores_both_fields_when_provided() {
        var ctx = new DefaultCorrelationContext("cid-1", "rid-1");

        assertThat(ctx.correlationId()).isEqualTo("cid-1");
        assertThat(ctx.requestId()).isEqualTo("rid-1");
    }

    @Test
    void implements_CorrelationContext() {
        CorrelationContext ctx = new DefaultCorrelationContext("cid-1", "rid-1");

        assertThat(ctx.correlationId()).isEqualTo("cid-1");
        assertThat(ctx.requestId()).isEqualTo("rid-1");
    }

    @Test
    void record_equality_by_value() {
        var a = new DefaultCorrelationContext("cid-1", "rid-1");
        var b = new DefaultCorrelationContext("cid-1", "rid-1");

        assertThat(a).isEqualTo(b);
    }
}
