package com.arbitrier.orchestrator.adapter.outbound;

import com.arbitrier.orchestrator.application.port.outbound.ReserveCreditCommandPublisher;
import com.arbitrier.orchestrator.application.port.outbound.ReserveCreditSagaCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * Test adapter: records published ReserveCredit commands for assertion in tests.
 *
 * <p>Layer: adapter/outbound (test)
 * <p>Module: orchestrator-service
 */
public class RecordingReserveCreditCommandPublisher implements ReserveCreditCommandPublisher {

    private final List<ReserveCreditSagaCommand> commands = new ArrayList<>();

    @Override
    public void publishReserveCredit(final ReserveCreditSagaCommand command) {
        commands.add(command);
    }

    public List<ReserveCreditSagaCommand> commands() {
        return List.copyOf(commands);
    }

    public int commandCount() {
        return commands.size();
    }
}
