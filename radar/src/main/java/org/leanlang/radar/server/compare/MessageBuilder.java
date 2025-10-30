package org.leanlang.radar.server.compare;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public final class MessageBuilder {
    List<JsonMessageSegment> sections = new ArrayList<>();

    public MessageBuilder add(JsonMessageSegment section) {
        sections.add(section);
        return this;
    }

    public MessageBuilder addDelta(float amount, @Nullable String unit, int direction) {
        return add(new JsonMessageSegment.Delta(amount, Optional.ofNullable(unit), direction));
    }

    public MessageBuilder addDeltaPercent(float factor, int direction) {
        return add(new JsonMessageSegment.DeltaPercent(factor, direction));
    }

    public MessageBuilder addExitCode(int exitCode) {
        return add(new JsonMessageSegment.ExitCode(exitCode));
    }

    public MessageBuilder addMetric(String metric) {
        return add(new JsonMessageSegment.Metric(metric));
    }

    public MessageBuilder addRun(String name) {
        return add(new JsonMessageSegment.Run(name));
    }

    public MessageBuilder addText(String text) {
        return add(new JsonMessageSegment.Text(text));
    }

    public List<JsonMessageSegment> build() {
        return sections.stream().toList();
    }
}
