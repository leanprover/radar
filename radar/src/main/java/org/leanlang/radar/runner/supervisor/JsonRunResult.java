package org.leanlang.radar.runner.supervisor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
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
        @NotNull Optional<Instant> scriptStartTime,
        @NotNull Optional<Instant> scriptEndTime,
        @NotNull int exitCode,
        @Valid @NotNull List<JsonRunResultEntry> entries,
        @Valid @NotNull List<JsonOutputLine> lines) {

    public JsonRunResult(
            Job job,
            Instant startTime,
            Instant endTime,
            @Nullable Instant scriptStartTime,
            @Nullable Instant scriptEndTime,
            int exitCode,
            List<JsonRunResultEntry> entries,
            List<OutputLine> lines) {
        this(
                job.repo(),
                job.chash(),
                job.benchChash(),
                job.name(),
                job.script(),
                startTime,
                endTime,
                Optional.ofNullable(scriptStartTime),
                Optional.ofNullable(scriptEndTime),
                exitCode,
                entries,
                lines.stream().map(JsonOutputLine::new).toList());
    }

    public RunResult toRunResult(String runner) {
        return new RunResult(
                chash,
                new Run(name, script, runner),
                benchChash,
                startTime,
                endTime,
                scriptStartTime,
                scriptEndTime,
                exitCode,
                entries.stream().map(JsonRunResultEntry::toRunResultEntry).toList(),
                lines);
    }
}
