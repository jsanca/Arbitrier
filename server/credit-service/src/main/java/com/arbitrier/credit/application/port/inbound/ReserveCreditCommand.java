package com.arbitrier.credit.application.port.inbound;

import com.arbitrier.credit.domain.model.Money;
import com.arbitrier.platform.validation.Require;

/**
 * Command to reserve credit for an order.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: credit-service
 */
public record ReserveCreditCommand(
        String orderId,
        String creditReservationId,
        String customerId,
        Money amount) {

    public ReserveCreditCommand {
        Require.notBlank(orderId, "ReserveCreditCommand.orderId");
        Require.notBlank(creditReservationId, "ReserveCreditCommand.creditReservationId");
        Require.notBlank(customerId, "ReserveCreditCommand.customerId");
        Require.notNull(amount, "ReserveCreditCommand.amount");
    }
}
