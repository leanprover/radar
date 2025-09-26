package org.leanlang.radar.runner.supervisor;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.server.queue.Job;
import org.leanlang.radar.server.queue.Run;
import org.leanlang.radar.server.queue.RunResult;

public record JsonRunResult(
        @JsonProperty(required = true) String repo,
        @JsonProperty(required = true) String chash,
        @JsonProperty(required = true) String benchChash,
        @JsonProperty(required = true) String name,
        @JsonProperty(required = true) String script,
        @JsonProperty(required = true) Instant startTime,
        @JsonProperty(required = true) Instant endTime,
        Optional<Instant> scriptStartTime,
        Optional<Instant> scriptEndTime,
        @JsonProperty(required = true) int exitCode,
        @JsonProperty(required = true) List<JsonRunResultEntry> entries,
        @JsonProperty(required = true) List<JsonOutputLine> lines) {

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
