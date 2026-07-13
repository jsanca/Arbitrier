package com.arbitrier.platform.messaging.outbox;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageNatureTest {

    @Test
    void event_and_command_values_exist() {
        assertThat(MessageNature.values()).containsExactlyInAnyOrder(
                MessageNature.EVENT, MessageNature.COMMAND);
    }

    @Test
    void valueOf_roundtrip() {
        assertThat(MessageNature.valueOf("EVENT")).isEqualTo(MessageNature.EVENT);
        assertThat(MessageNature.valueOf("COMMAND")).isEqualTo(MessageNature.COMMAND);
    }

    @Test
    void event_is_not_command() {
        assertThat(MessageNature.EVENT).isNotEqualTo(MessageNature.COMMAND);
    }
}
