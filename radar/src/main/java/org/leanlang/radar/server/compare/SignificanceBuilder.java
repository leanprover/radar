package org.leanlang.radar.server.compare;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public final class SignificanceBuilder {
    private int importance;
    private int goodness = 0;
    private final List<JsonMessageSegment> segments = new ArrayList<>();

    public SignificanceBuilder(int importance) {
        this.importance = importance;
    }

    public int importance() {
        return importance;
    }

    public SignificanceBuilder setImportance(int importance) {
        this.importance = importance;
        return this;
    }

    public int goodness() {
        return goodness;
    }

    public SignificanceBuilder setGoodness(int goodness) {
        this.goodness = goodness;
        return this;
    }

    public SignificanceBuilder setGoodnessWithDirection(int direction, float value) {
        if ((direction > 0 && value > 0) || (direction < 0 && value < 0))
            return setGoodness(JsonSignificance.GOODNESS_GOOD);
        if ((direction > 0 && value < 0) || (direction < 0 && value > 0))
            return setGoodness(JsonSignificance.GOODNESS_BAD);
        return setGoodness(JsonSignificance.GOODNESS_NEUTRAL);
    }

    public SignificanceBuilder add(JsonMessageSegment section) {
        segments.add(section);
        return this;
    }

    public SignificanceBuilder addDelta(float amount, @Nullable String unit, OldMessageGoodness goodness) {
        return add(new JsonMessageSegment.Delta(amount, Optional.ofNullable(unit), goodness));
    }

    public SignificanceBuilder addDelta(float amount, @Nullable String unit, int direction) {
        return addDelta(amount, unit, OldMessageGoodness.fromDelta(amount, direction));
    }

    public SignificanceBuilder addDeltaPercent(float factor, OldMessageGoodness goodness) {
        return add(new JsonMessageSegment.DeltaPercent(factor, goodness));
    }

    public SignificanceBuilder addDeltaPercent(float factor, int direction) {
        return addDeltaPercent(factor, OldMessageGoodness.fromDelta(factor, direction));
    }

    public SignificanceBuilder addDeltaAndDeltaPercent(
            float first, float second, @Nullable String unit, int direction) {
        addDelta(second - first, unit, direction);

        if (first != 0) {
            addText(" (");
            addDeltaPercent((second - first) / first, direction);
            addText(")");
        }

        return this;
    }

    public SignificanceBuilder addExitCode(int exitCode) {
        return add(new JsonMessageSegment.ExitCode(
                exitCode, exitCode == 0 ? OldMessageGoodness.GOOD : OldMessageGoodness.BAD));
    }

    public SignificanceBuilder addMetric(String metric) {
        return add(new JsonMessageSegment.Metric(metric));
    }

    public SignificanceBuilder addRun(String name) {
        return add(new JsonMessageSegment.Run(name));
    }

    public SignificanceBuilder addText(String text) {
        return add(new JsonMessageSegment.Text(text));
    }

    public JsonSignificance build() {
        return new JsonSignificance(importance, goodness, segments.stream().toList());
    }

    public Optional<JsonSignificance> buildOpt() {
        return Optional.of(build());
    }
}
