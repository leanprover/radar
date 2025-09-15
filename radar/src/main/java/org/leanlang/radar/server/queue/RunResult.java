package org.leanlang.radar.server.queue;

import java.util.List;

public record RunResult(
        String repo,
        String chash,
        String benchChash,
        String runner,
        String script,
        int exitCode,
        List<RunResultEntry> entries) {}
