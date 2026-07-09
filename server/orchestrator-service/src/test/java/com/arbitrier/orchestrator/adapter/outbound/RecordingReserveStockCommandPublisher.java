package com.arbitrier.orchestrator.adapter.outbound;

import com.arbitrier.orchestrator.application.port.outbound.ReserveStockCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.ReserveStockSagaCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * Test adapter: records published ReserveStock commands for assertion in tests.
 *
 * <p>Layer: adapter/outbound (test)
 * <p>Module: orchestrator-service
 */
public class RecordingReserveStockCommandPublisher implements ReserveStockCommandPublisher {

    private final List<ReserveStockSagaCommand> commands = new ArrayList<>();

    @Override
    public void publishReserveStock(final ReserveStockSagaCommand command) {
        commands.add(command);
    }

    public List<ReserveStockSagaCommand> commands() {
        return List.copyOf(commands);
    }

    public int commandCount() {
        return commands.size();
    }
}
