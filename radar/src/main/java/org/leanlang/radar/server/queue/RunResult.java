package org.leanlang.radar.server.queue;

import java.time.Instant;
import java.util.List;
import org.leanlang.radar.runner.supervisor.JsonOutputLine;

public record RunResult(
        String chash,
        Run run,
        String benchChash,
        Instant startTime,
        Instant endTime,
        int exitCode,
        List<RunResultEntry> entries,
        List<JsonOutputLine> lines) {}
