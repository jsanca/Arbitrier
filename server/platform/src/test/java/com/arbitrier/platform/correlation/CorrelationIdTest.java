package com.arbitrier.platform.correlation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class CorrelationIdTest {

    @Test
    void generate_produces_non_blank_value() {
        CorrelationId id = CorrelationId.generate();
        assertThat(id.value()).isNotBlank();
    }

    @Test
    void generate_produces_unique_values() {
        assertThat(CorrelationId.generate()).isNotEqualTo(CorrelationId.generate());
    }

    @Test
    void of_wraps_provided_value() {
        CorrelationId id = CorrelationId.of("abc-123");
        assertThat(id.value()).isEqualTo("abc-123");
    }

    @Test
    void constructor_rejects_null() {
        assertThatNullPointerException().isThrownBy(() -> CorrelationId.of(null));
    }

    @Test
    void constructor_rejects_blank() {
        assertThatIllegalArgumentException().isThrownBy(() -> CorrelationId.of("   "));
    }

    @Test
    void toString_returns_value() {
        CorrelationId id = CorrelationId.of("my-id");
        assertThat(id.toString()).isEqualTo("my-id");
    }

    @Test
    void equality_is_value_based() {
        assertThat(CorrelationId.of("same")).isEqualTo(CorrelationId.of("same"));
        assertThat(CorrelationId.of("a")).isNotEqualTo(CorrelationId.of("b"));
    }

    @Test
    void same_pattern_holds_for_causation_message_request() {
        assertThat(CausationId.generate().value()).isNotBlank();
        assertThat(MessageId.generate().value()).isNotBlank();
        assertThat(RequestId.generate().value()).isNotBlank();
    }
}
