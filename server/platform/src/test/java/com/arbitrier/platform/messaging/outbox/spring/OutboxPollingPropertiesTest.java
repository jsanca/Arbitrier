package com.arbitrier.platform.messaging.outbox.spring;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OutboxPollingProperties} default values and mutation.
 * No Spring context — verifies the POJO directly.
 */
class OutboxPollingPropertiesTest {

    @Test
    void default_enabled_is_true() {
        assertThat(new OutboxPollingProperties().isEnabled()).isTrue();
    }

    @Test
    void default_fixed_delay_is_10_seconds() {
        assertThat(new OutboxPollingProperties().getFixedDelayMs()).isEqualTo(10_000L);
    }

    @Test
    void default_initial_delay_is_5_seconds() {
        assertThat(new OutboxPollingProperties().getInitialDelayMs()).isEqualTo(5_000L);
    }

    @Test
    void default_batch_size_is_100() {
        assertThat(new OutboxPollingProperties().getBatchSize()).isEqualTo(100);
    }

    @Test
    void custom_values_are_applied() {
        var props = new OutboxPollingProperties();
        props.setEnabled(false);
        props.setFixedDelayMs(3_000);
        props.setInitialDelayMs(1_000);
        props.setBatchSize(50);

        assertThat(props.isEnabled()).isFalse();
        assertThat(props.getFixedDelayMs()).isEqualTo(3_000L);
        assertThat(props.getInitialDelayMs()).isEqualTo(1_000L);
        assertThat(props.getBatchSize()).isEqualTo(50);
    }
}
