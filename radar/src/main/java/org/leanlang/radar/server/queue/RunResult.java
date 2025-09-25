package org.leanlang.radar.server.queue;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.leanlang.radar.runner.supervisor.JsonOutputLine;

public record RunResult(
        String chash,
        Run run,
        String benchChash,
        Instant startTime,
        Instant endTime,
        Optional<Instant> scriptStartTime,
        Optional<Instant> scriptEndTime,
        int exitCode,
        List<RunResultEntry> entries,
        List<JsonOutputLine> lines) {}
