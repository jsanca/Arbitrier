package com.arbitrier.credit.domain.model;

import com.arbitrier.platform.validation.Require;
import java.math.BigDecimal;

/**
 * A non-negative monetary amount with an ISO-4217 currency code, scoped to the credit context.
 *
 * <p>Layer: domain/model
 * <p>Module: credit-service
 */
public record Money(BigDecimal amount, String currency) {
    public Money {
        Require.notNull(amount, "Money.amount");
        Require.notBlank(currency, "Money.currency");
        Require.isTrue(amount.compareTo(BigDecimal.ZERO) >= 0,
                "Money.amount must be zero or positive, got: " + amount);
    }

    /** Creates a {@code Money} instance with the given amount and currency code. */
    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }

    /**
     * Returns a new {@code Money} representing the sum of this and {@code other}.
     *
     * @param other the amount to add; must use the same currency
     * @throws NullPointerException     if {@code other} is null
     * @throws IllegalArgumentException if the currencies differ
     */
    public Money add(Money other) {
        Require.notNull(other, "other");
        Require.isTrue(currency.equals(other.currency),
                "Currency mismatch: " + currency + " vs " + other.currency);
        return new Money(amount.add(other.amount), currency);
    }

    /**
     * Returns {@code true} if this amount is sufficient to cover the {@code requested} amount.
     *
     * <p>Currencies must match; different currencies fail fast rather than performing FX conversion.
     *
     * @param requested the amount that needs to be covered
     * @throws NullPointerException     if {@code requested} is null
     * @throws IllegalArgumentException if the currencies differ
     */
    public boolean canCover(Money requested) {
        Require.notNull(requested, "requested");
        Require.isTrue(currency.equals(requested.currency),
                "Currency mismatch: " + currency + " vs " + requested.currency);
        return amount.compareTo(requested.amount) >= 0;
    }
}
