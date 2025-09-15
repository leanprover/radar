package org.leanlang.radar.runner.supervisor;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import org.leanlang.radar.server.queue.RunResult;

public record JsonRunResult(
        @NotNull String repo,
        @NotNull String chash,
        @NotNull String benchChash,
        @NotNull String script,
        @NotNull Instant startTime,
        @NotNull Instant endTime,
        @NotNull int exitCode,
        @NotNull List<JsonRunResultEntry> entries) {
    public RunResult toRunResult(String runner) {
        return new RunResult(
                chash,
                runner,
                script,
                benchChash,
                startTime,
                endTime,
                exitCode,
                entries.stream().map(JsonRunResultEntry::toRunResultEntry).toList());
    }
}
