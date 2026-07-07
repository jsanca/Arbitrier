package com.arbitrier.platform.idempotency;

import com.arbitrier.platform.test.FixedClock;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class IdempotencyTest {

    private static final Instant NOW = FixedClock.TEST_INSTANT;

    // ── IdempotencyKey ────────────────────────────────────────────────────────

    @Test
    void key_generate_produces_non_blank_value() {
        assertThat(IdempotencyKey.generate().value()).isNotBlank();
    }

    @Test
    void key_of_wraps_value() {
        assertThat(IdempotencyKey.of("k1").value()).isEqualTo("k1");
    }

    @Test
    void key_rejects_null() {
        assertThatNullPointerException().isThrownBy(() -> IdempotencyKey.of(null));
    }

    @Test
    void key_rejects_blank() {
        assertThatIllegalArgumentException().isThrownBy(() -> IdempotencyKey.of(""));
    }

    // ── IdempotencyRecord ─────────────────────────────────────────────────────

    @Test
    void pending_record_has_null_processed_at() {
        IdempotencyKey key = IdempotencyKey.of("k1");
        IdempotencyRecord record = IdempotencyRecord.pending(key, NOW);

        assertThat(record.status()).isEqualTo(IdempotencyStatus.PENDING);
        assertThat(record.processedAt()).isNull();
        assertThat(record.isTerminal()).isFalse();
    }

    @Test
    void mark_processed_transitions_to_terminal() {
        IdempotencyRecord record = IdempotencyRecord
                .pending(IdempotencyKey.of("k2"), NOW)
                .markProcessed(NOW.plusSeconds(1));

        assertThat(record.status()).isEqualTo(IdempotencyStatus.PROCESSED);
        assertThat(record.processedAt()).isEqualTo(NOW.plusSeconds(1));
        assertThat(record.isTerminal()).isTrue();
    }

    @Test
    void mark_failed_transitions_to_terminal() {
        IdempotencyRecord record = IdempotencyRecord
                .pending(IdempotencyKey.of("k3"), NOW)
                .markFailed(NOW.plusSeconds(2));

        assertThat(record.status()).isEqualTo(IdempotencyStatus.FAILED);
        assertThat(record.isTerminal()).isTrue();
    }

    @Test
    void mark_processed_rejects_null_timestamp() {
        IdempotencyRecord pending = IdempotencyRecord.pending(IdempotencyKey.of("k4"), NOW);
        assertThatNullPointerException().isThrownBy(() -> pending.markProcessed(null));
    }

    @Test
    void record_is_immutable_on_state_transition() {
        IdempotencyRecord pending = IdempotencyRecord.pending(IdempotencyKey.of("k5"), NOW);
        IdempotencyRecord processed = pending.markProcessed(NOW.plusSeconds(5));

        assertThat(pending.status()).isEqualTo(IdempotencyStatus.PENDING);
        assertThat(processed.status()).isEqualTo(IdempotencyStatus.PROCESSED);
    }
}
