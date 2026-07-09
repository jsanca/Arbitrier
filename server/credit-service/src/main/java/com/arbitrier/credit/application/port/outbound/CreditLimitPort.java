package com.arbitrier.credit.application.port.outbound;

import com.arbitrier.credit.domain.model.Money;

/**
 * Outbound port: queries available credit for a customer.
 *
 * <p>Layer: application/port/outbound
 * <p>Module: credit-service
 */
public interface CreditLimitPort {

    /**
     * Returns the available credit for the given customer.
     *
     * <p>OPEN QUESTION: behaviour when requested currency differs from the credit limit currency
     * must be defined before the JPA adapter is implemented.
     *
     * @param customerId the customer identifier
     * @return the currently available credit amount
     */
    Money availableCredit(String customerId);
}
