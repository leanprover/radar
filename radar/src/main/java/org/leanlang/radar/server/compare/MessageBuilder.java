package org.leanlang.radar.server.compare;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public final class MessageBuilder {
    private final JsonMessageGoodness goodness;
    private final List<JsonMessageSegment> sections = new ArrayList<>();

    public MessageBuilder(JsonMessageGoodness goodness) {
        this.goodness = goodness;
    }

    public MessageBuilder add(JsonMessageSegment section) {
        sections.add(section);
        return this;
    }

    public MessageBuilder addDelta(float amount, @Nullable String unit, JsonMessageGoodness goodness) {
        return add(new JsonMessageSegment.Delta(amount, Optional.ofNullable(unit), goodness));
    }

    public MessageBuilder addDelta(float amount, @Nullable String unit, int direction) {
        return addDelta(amount, unit, JsonMessageGoodness.fromDelta(amount, direction));
    }

    public MessageBuilder addDeltaPercent(float factor, JsonMessageGoodness goodness) {
        return add(new JsonMessageSegment.DeltaPercent(factor, goodness));
    }

    public MessageBuilder addDeltaPercent(float factor, int direction) {
        return addDeltaPercent(factor, JsonMessageGoodness.fromDelta(factor, direction));
    }

    public MessageBuilder addDeltaAndDeltaPercent(float first, float second, @Nullable String unit, int direction) {
        addDelta(second - first, unit, direction);

        if (first != 0) {
            addText(" (");
            addDeltaPercent((second - first) / first, direction);
            addText(")");
        }

        return this;
    }

    public MessageBuilder addExitCode(int exitCode) {
        return add(new JsonMessageSegment.ExitCode(
                exitCode, exitCode == 0 ? JsonMessageGoodness.GOOD : JsonMessageGoodness.BAD));
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

    public JsonMessage build() {
        return new JsonMessage(goodness, sections.stream().toList());
    }
}
