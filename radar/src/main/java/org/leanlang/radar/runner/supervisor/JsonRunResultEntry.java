package org.leanlang.radar.runner.supervisor;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;
import org.leanlang.radar.server.queue.RunResultEntry;

public record JsonRunResultEntry(
        @JsonProperty(required = true) String metric,
        @JsonProperty(required = true) float value,
        Optional<String> unit,
        Optional<Integer> direction) {

    public RunResultEntry toRunResultEntry() {
        return new RunResultEntry(metric, value, unit, direction);
    }
}
