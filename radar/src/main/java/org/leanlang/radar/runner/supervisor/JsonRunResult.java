package org.leanlang.radar.runner.supervisor;

import java.util.List;

public record JsonRunResult(
        String repo, String chash, String benchChash, String script, int exitCode, List<JsonRunResultEntry> entries) {}
