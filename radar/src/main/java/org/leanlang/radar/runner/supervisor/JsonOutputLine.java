package org.leanlang.radar.runner.supervisor;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public record JsonOutputLine(
        @JsonProperty(required = true) Instant time,
        @JsonProperty(required = true) int source,
        @JsonProperty(required = true) String line) {
    public JsonOutputLine(OutputLine outputLine) {
        this(outputLine.time(), outputLine.source(), outputLine.line());
    }
}
