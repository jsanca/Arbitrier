package com.arbitrier.platform.correlation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class CorrelationContextHolderTest {

    @AfterEach
    void cleanup() {
        CorrelationContextHolder.clear();
    }

    @Test
    void get_returns_empty_when_nothing_set() {
        assertThat(CorrelationContextHolder.get()).isEmpty();
    }

    @Test
    void get_returns_context_after_set() {
        var ctx = new DefaultCorrelationContext("cid-1", "rid-1");

        CorrelationContextHolder.set(ctx);

        assertThat(CorrelationContextHolder.get()).contains(ctx);
    }

    @Test
    void clear_removes_context() {
        CorrelationContextHolder.set(new DefaultCorrelationContext("cid-1", null));

        CorrelationContextHolder.clear();

        assertThat(CorrelationContextHolder.get()).isEmpty();
    }

    @Test
    void set_replaces_existing_context() {
        var first  = new DefaultCorrelationContext("cid-1", "rid-1");
        var second = new DefaultCorrelationContext("cid-2", "rid-2");

        CorrelationContextHolder.set(first);
        CorrelationContextHolder.set(second);

        assertThat(CorrelationContextHolder.get()).contains(second);
    }

    @Test
    void set_rejects_null() {
        assertThatNullPointerException()
                .isThrownBy(() -> CorrelationContextHolder.set(null));
    }

    @Test
    void clear_is_idempotent_when_nothing_set() {
        CorrelationContextHolder.clear();
        CorrelationContextHolder.clear();

        assertThat(CorrelationContextHolder.get()).isEmpty();
    }
}
