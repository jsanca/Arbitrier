package com.arbitrier.order.application.port.inbound;

import com.arbitrier.platform.validation.Require;
import java.util.List;

/**
 * Application command for submitting a corporate bulk order.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: order-service
 */
public record SubmitCorporateBulkOrderCommand(
        String customerId,
        String submittedByUserId,
        List<SubmitCorporateBulkOrderLineCommand> lines) {

    public SubmitCorporateBulkOrderCommand {
        Require.notBlank(customerId, "customerId");
        Require.notBlank(submittedByUserId, "submittedByUserId");
        Require.notEmpty(lines, "lines");
        lines = List.copyOf(lines);
    }
}
