package com.arbitrier.orchestrator.adapter.outbound;

import com.arbitrier.orchestrator.application.port.outbound.ConfirmOrderCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.ConfirmOrderSagaCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * Test adapter: records published ConfirmOrder commands for assertion in tests.
 *
 * <p>Layer: adapter/outbound (test)
 * <p>Module: orchestrator-service
 */
public class RecordingConfirmOrderCommandPublisher implements ConfirmOrderCommandPublisher {

    private final List<ConfirmOrderSagaCommand> commands = new ArrayList<>();

    @Override
    public void publishConfirmOrder(final ConfirmOrderSagaCommand command) {
        commands.add(command);
    }

    public List<ConfirmOrderSagaCommand> commands() {
        return List.copyOf(commands);
    }

    public int commandCount() {
        return commands.size();
    }
}
