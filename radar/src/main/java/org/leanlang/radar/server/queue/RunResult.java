package org.leanlang.radar.server.queue;

import java.time.Instant;
import java.util.List;

public record RunResult(
        String chash,
        String runner,
        String script,
        String benchChash,
        Instant startTime,
        Instant endTime,
        int exitCode,
        List<RunResultEntry> entries) {}
