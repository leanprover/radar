package org.leanlang.radar.runner.supervisor;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import org.leanlang.radar.server.queue.Job;
import org.leanlang.radar.server.queue.Run;
import org.leanlang.radar.server.queue.RunResult;

public record JsonRunResult(
        @NotNull String repo,
        @NotNull String chash,
        @NotNull String benchChash,
        @NotNull String name,
        @NotNull String script,
        @NotNull Instant startTime,
        @NotNull Instant endTime,
        @NotNull int exitCode,
        @NotNull List<JsonRunResultEntry> entries) {
    public JsonRunResult(Job job, Instant startTime, Instant endTime, int exitCode, List<JsonRunResultEntry> entries) {
        this(
                job.repo(),
                job.chash(),
                job.benchChash(),
                job.name(),
                job.script(),
                startTime,
                endTime,
                exitCode,
                entries);
    }

    public JsonRunResult(Job job, Instant startTime, Instant endTime, int exitCode) {
        this(job, startTime, endTime, exitCode, List.of());
    }

    public RunResult toRunResult(String runner) {
        return new RunResult(
                chash,
                new Run(name, script, runner),
                benchChash,
                startTime,
                endTime,
                exitCode,
                entries.stream().map(JsonRunResultEntry::toRunResultEntry).toList());
    }
}
