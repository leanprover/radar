package org.leanlang.radar.server.queue;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import org.leanlang.radar.codegen.jooq.tables.records.RunsRecord;

public record JsonRun(
        @JsonProperty(required = true) String name,
        @JsonProperty(required = true) String script,
        @JsonProperty(required = true) String runner,
        Optional<Active> active,
        Optional<Finished> finished)
        implements Comparable<JsonRun> {

    private static final Comparator<JsonRun> comparator =
            Comparator.comparing(JsonRun::name).thenComparing(JsonRun::script).thenComparing(JsonRun::runner);

    @Override
    public int compareTo(JsonRun o) {
        return comparator.compare(this, o);
    }

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

    public JsonRun(RunsRecord record) {
        this(
                record.getName(),
                record.getScript(),
                record.getRunner(),
                Optional.empty(),
                Optional.of(new Finished(record.getStartTime(), record.getEndTime(), record.getExitCode())));
    }
}
