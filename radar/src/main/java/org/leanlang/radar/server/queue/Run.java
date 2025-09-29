package org.leanlang.radar.server.queue;

import java.time.Instant;
import java.util.Optional;
import org.leanlang.radar.runner.supervisor.JsonOutputLineBatch;

public record Run(String name, String script, String runner, Optional<Active> active, Optional<Finished> finished) {

    public record Active(String benchChash, Instant startTime, JsonOutputLineBatch lines) {}

    public record Finished(
            String benchChash,
            Instant startTime,
            Instant endTime,
            Optional<Instant> scriptStartTime,
            Optional<Instant> scriptEndTime,
            int exitCode) {}
}
