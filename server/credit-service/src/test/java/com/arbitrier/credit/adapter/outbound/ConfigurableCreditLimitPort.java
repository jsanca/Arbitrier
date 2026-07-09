package com.arbitrier.credit.adapter.outbound;

import com.arbitrier.credit.application.port.outbound.CreditLimitPort;
import com.arbitrier.credit.domain.model.Money;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Test adapter: configurable per-customer credit limit for unit and integration tests.
 *
 * <p>Defaults to zero available credit for any unconfigured customer.
 *
 * <p>Layer: adapter/outbound (test)
 * <p>Module: credit-service
 */
public class ConfigurableCreditLimitPort implements CreditLimitPort {

    private static final String DEFAULT_CURRENCY = "USD";

    private final Map<String, Money> limits = new HashMap<>();

    /** Sets available credit for a specific customer. */
    public void setAvailableCredit(String customerId, Money available) {
        limits.put(customerId, available);
    }

    /** Convenience: grants effectively unlimited credit to multiple customers. */
    public void setUnlimited(String... customerIds) {
        final Money unlimited = Money.of(new BigDecimal("999999999.00"), DEFAULT_CURRENCY);
        for (String id : customerIds) {
            limits.put(id, unlimited);
        }
    }

    @Override
    public Money availableCredit(String customerId) {
        return limits.getOrDefault(customerId, Money.of(BigDecimal.ZERO, DEFAULT_CURRENCY));
    }
}
