package com.arbitrier.credit.domain;

import com.arbitrier.credit.domain.model.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Unit tests for {@link Money}.
 */
class MoneyTest {

    private static final Money ONE_THOUSAND_USD = Money.of(new BigDecimal("1000.00"), "USD");
    private static final Money FIVE_HUNDRED_USD = Money.of(new BigDecimal("500.00"), "USD");
    private static final Money ONE_THOUSAND_EUR = Money.of(new BigDecimal("1000.00"), "EUR");

    // ── canCover ──────────────────────────────────────────────────────────────

    @Test
    void canCover_returns_true_when_available_equals_requested() {
        assertThat(ONE_THOUSAND_USD.canCover(ONE_THOUSAND_USD)).isTrue();
    }

    @Test
    void canCover_returns_true_when_available_exceeds_requested() {
        assertThat(ONE_THOUSAND_USD.canCover(FIVE_HUNDRED_USD)).isTrue();
    }

    @Test
    void canCover_returns_false_when_available_less_than_requested() {
        assertThat(FIVE_HUNDRED_USD.canCover(ONE_THOUSAND_USD)).isFalse();
    }

    @Test
    void canCover_throws_on_currency_mismatch() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ONE_THOUSAND_USD.canCover(ONE_THOUSAND_EUR))
                .withMessageContaining("Currency mismatch")
                .withMessageContaining("USD")
                .withMessageContaining("EUR");
    }

    @Test
    void canCover_throws_on_null_requested() {
        assertThatNullPointerException()
                .isThrownBy(() -> ONE_THOUSAND_USD.canCover(null))
                .withMessageContaining("requested");
    }
}
