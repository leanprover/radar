package org.leanlang.radar.server.queue;

import java.time.Instant;
import java.util.List;

public record RunResult(
        String chash,
        Run run,
        String benchChash,
        Instant startTime,
        Instant endTime,
        int exitCode,
        List<RunResultEntry> entries) {}
