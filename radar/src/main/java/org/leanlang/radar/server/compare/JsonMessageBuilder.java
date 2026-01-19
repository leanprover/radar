package org.leanlang.radar.server.compare;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public final class JsonMessageBuilder {
    private boolean hidden = false;
    private JsonMessageGoodness goodness = JsonMessageGoodness.NEUTRAL;
    private final List<JsonMessageSegment> segments = new ArrayList<>();

    public JsonMessageBuilder setHidden(boolean hidden) {
        this.hidden = hidden;
        return this;
    }

    public JsonMessageBuilder setGoodness(JsonMessageGoodness goodness) {
        this.goodness = goodness;
        return this;
    }

    public JsonMessageBuilder add(JsonMessageSegment section) {
        segments.add(section);
        return this;
    }

    public JsonMessageBuilder addDelta(float amount, @Nullable String unit, JsonMessageGoodness goodness) {
        return add(new JsonMessageSegment.Delta(amount, Optional.ofNullable(unit), goodness));
    }

    public JsonMessageBuilder addDelta(float amount, @Nullable String unit, int direction) {
        return addDelta(amount, unit, JsonMessageGoodness.fromDelta(amount, direction));
    }

    public JsonMessageBuilder addDeltaPercent(float factor, JsonMessageGoodness goodness) {
        return add(new JsonMessageSegment.DeltaPercent(factor, goodness));
    }

    public JsonMessageBuilder addDeltaPercent(float factor, int direction) {
        return addDeltaPercent(factor, JsonMessageGoodness.fromDelta(factor, direction));
    }

    public JsonMessageBuilder addDeltaAndDeltaPercent(float first, float second, @Nullable String unit, int direction) {
        addDelta(second - first, unit, direction);

        if (first != 0) {
            addText(" (");
            addDeltaPercent((second - first) / first, direction);
            addText(")");
        }

        return this;
    }

    public JsonMessageBuilder addExitCode(int exitCode) {
        return add(new JsonMessageSegment.ExitCode(
                exitCode, exitCode == 0 ? JsonMessageGoodness.GOOD : JsonMessageGoodness.BAD));
    }

    public JsonMessageBuilder addMetric(String metric) {
        return add(new JsonMessageSegment.Metric(metric));
    }

    public JsonMessageBuilder addRun(String name) {
        return add(new JsonMessageSegment.Run(name));
    }

    public JsonMessageBuilder addText(String text) {
        return add(new JsonMessageSegment.Text(text));
    }

    public static JsonMessageBuilder metricDeltaDeltaPercentGoodness(
            String metric, float first, float second, @Nullable String unit, int direction) {
        return new JsonMessageBuilder()
                .addMetric(metric)
                .addText(": ")
                .addDeltaAndDeltaPercent(first, second, unit, direction)
                .setGoodness(JsonMessageGoodness.fromDelta(second - first, direction));
    }

    public JsonMessage build() {
        return new JsonMessage(hidden, goodness, segments.stream().toList());
    }
}
