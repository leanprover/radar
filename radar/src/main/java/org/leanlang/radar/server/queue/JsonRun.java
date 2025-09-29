package org.leanlang.radar.server.queue;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Optional;

public record JsonRun(
        @JsonProperty(required = true) String name,
        @JsonProperty(required = true) String script,
        @JsonProperty(required = true) String runner,
        Optional<Active> active,
        Optional<Finished> finished) {

    public record Active(@JsonProperty(required = true) Instant startTime) {}

    public record Finished(
            @JsonProperty(required = true) Instant startTime,
            @JsonProperty(required = true) Instant endTime,
            @JsonProperty(required = true) int exitCode) {}

    public JsonRun(Run run) {
        this(
                run.name(),
                run.script(),
                run.runner(),
                run.active().map(it -> new Active(it.startTime())),
                run.finished().map(it -> new Finished(it.startTime(), it.endTime(), it.exitCode())));
    }
}
