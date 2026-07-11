package com.arbitrier.orchestrator.adapter.outbound;

import com.arbitrier.orchestrator.application.port.outbound.ReleaseStockCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.ReleaseStockSagaCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * Test adapter: records published ReleaseStock commands for assertion in tests.
 *
 * <p>Layer: adapter/outbound (test)
 * <p>Module: orchestrator-service
 */
public class RecordingReleaseStockCommandPublisher implements ReleaseStockCommandPublisher {

    private final List<ReleaseStockSagaCommand> commands = new ArrayList<>();

    @Override
    public void publishReleaseStock(final ReleaseStockSagaCommand command) {
        commands.add(command);
    }

    public List<ReleaseStockSagaCommand> commands() {
        return List.copyOf(commands);
    }

    public int commandCount() {
        return commands.size();
    }
}
