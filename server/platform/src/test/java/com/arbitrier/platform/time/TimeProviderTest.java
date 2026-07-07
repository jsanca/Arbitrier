package com.arbitrier.platform.time;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class TimeProviderTest {

    @Test
    void system_clock_returns_non_null_instant() {
        assertThat(SystemClock.INSTANCE.now()).isNotNull();
    }

    @Test
    void system_clock_singleton_is_consistent() {
        assertThat(SystemClock.INSTANCE).isSameAs(SystemClock.INSTANCE);
    }

    @Test
    void fixed_provider_returns_pinned_instant() {
        Instant fixed = Instant.parse("2026-01-15T10:00:00Z");
        TimeProvider provider = FixedTimeProvider.of(fixed);

        assertThat(provider.now()).isEqualTo(fixed);
        assertThat(provider.now()).isEqualTo(fixed);
    }

    @Test
    void fixed_provider_today_uses_pinned_instant() {
        Instant fixed = Instant.parse("2026-01-15T10:00:00Z");
        TimeProvider provider = FixedTimeProvider.of(fixed);

        assertThat(provider.today(ZoneId.of("UTC")).toString()).isEqualTo("2026-01-15");
    }

    @Test
    void fixed_provider_rejects_null_instant() {
        assertThatNullPointerException().isThrownBy(() -> FixedTimeProvider.of(null));
    }
}
