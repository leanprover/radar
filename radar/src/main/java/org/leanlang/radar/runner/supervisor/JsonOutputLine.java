package org.leanlang.radar.runner.supervisor;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public record JsonOutputLine(Instant time, int source, String line) {
    public JsonOutputLine(OutputLine outputLine) {
        this(outputLine.time(), outputLine.source(), outputLine.line());
    }
}
