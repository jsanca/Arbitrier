package com.arbitrier.orchestrator.adapter.outbound;

import com.arbitrier.orchestrator.application.port.outbound.ReleaseCreditCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.ReleaseCreditSagaCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * Test adapter: records published ReleaseCredit commands for assertion in tests.
 *
 * <p>Layer: adapter/outbound (test)
 * <p>Module: orchestrator-service
 */
public class RecordingReleaseCreditCommandPublisher implements ReleaseCreditCommandPublisher {

    private final List<ReleaseCreditSagaCommand> commands = new ArrayList<>();

    @Override
    public void publishReleaseCredit(final ReleaseCreditSagaCommand command) {
        commands.add(command);
    }

    public List<ReleaseCreditSagaCommand> commands() {
        return List.copyOf(commands);
    }

    public int commandCount() {
        return commands.size();
    }
}
