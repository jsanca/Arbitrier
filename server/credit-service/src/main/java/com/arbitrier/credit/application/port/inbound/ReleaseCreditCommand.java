package com.arbitrier.credit.application.port.inbound;

import com.arbitrier.platform.validation.Require;

/**
 * Command to release an approved credit reservation back to the credit line.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: credit-service
 */
public record ReleaseCreditCommand(String creditReservationId) {

    public ReleaseCreditCommand {
        Require.notBlank(creditReservationId, "ReleaseCreditCommand.creditReservationId");
    }
}
