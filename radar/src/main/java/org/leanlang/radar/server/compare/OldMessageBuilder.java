package org.leanlang.radar.server.compare;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

// TODO Remove once old significance computation is obsolete
public final class OldMessageBuilder {
    private final OldMessageGoodness goodness;
    private final List<JsonMessageSegment> sections = new ArrayList<>();

    public OldMessageBuilder(OldMessageGoodness goodness) {
        this.goodness = goodness;
    }

    public OldMessageBuilder add(JsonMessageSegment section) {
        sections.add(section);
        return this;
    }

    public OldMessageBuilder addDelta(float amount, @Nullable String unit, OldMessageGoodness goodness) {
        return add(new JsonMessageSegment.Delta(amount, Optional.ofNullable(unit), goodness));
    }

    public OldMessageBuilder addDelta(float amount, @Nullable String unit, int direction) {
        return addDelta(amount, unit, OldMessageGoodness.fromDelta(amount, direction));
    }

    public OldMessageBuilder addDeltaPercent(float factor, OldMessageGoodness goodness) {
        return add(new JsonMessageSegment.DeltaPercent(factor, goodness));
    }

    public OldMessageBuilder addDeltaPercent(float factor, int direction) {
        return addDeltaPercent(factor, OldMessageGoodness.fromDelta(factor, direction));
    }

    public OldMessageBuilder addDeltaAndDeltaPercent(float first, float second, @Nullable String unit, int direction) {
        addDelta(second - first, unit, direction);

        if (first != 0) {
            addText(" (");
            addDeltaPercent((second - first) / first, direction);
            addText(")");
        }

        return this;
    }

    public OldMessageBuilder addExitCode(int exitCode) {
        return add(new JsonMessageSegment.ExitCode(
                exitCode, exitCode == 0 ? OldMessageGoodness.GOOD : OldMessageGoodness.BAD));
    }

    public OldMessageBuilder addMetric(String metric) {
        return add(new JsonMessageSegment.Metric(metric));
    }

    public OldMessageBuilder addRun(String name) {
        return add(new JsonMessageSegment.Run(name));
    }

    public OldMessageBuilder addText(String text) {
        return add(new JsonMessageSegment.Text(text));
    }

    public OldJsonMessage build() {
        return new OldJsonMessage(goodness, sections.stream().toList());
    }
}
