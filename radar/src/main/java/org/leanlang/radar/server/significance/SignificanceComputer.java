package org.leanlang.radar.server.significance;

import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.server.repos.RepoMetricMetadata;

public final class SignificanceComputer {
    public static Optional<MetricMessage> compareMetric(
            String metric,
            @Nullable String unit,
            RepoMetricMetadata metadata,
            @Nullable Float first,
            @Nullable Float second) {

        if (first == null && second == null)
            throw new IllegalArgumentException("first and second must not both be null");

        // majorAppear, minorAppear
        if (first == null)
            return msg(
                    metadata.majorAppear(),
                    metadata.minorAppear(),
                    new MessageBuilder()
                            .addMetric(metric)
                            .addText(" has appeared.")
                            .build());

        // majorDisappear, minorDisappear
        if (second == null)
            return msg(
                    metadata.majorDisappear(),
                    metadata.minorDisappear(),
                    new MessageBuilder()
                            .addMetric(metric)
                            .addText(" has disappeared.")
                            .build());

        return compareMetricWithValues(metric, unit, metadata, first, second);
    }

    private static Optional<MetricMessage> compareMetricWithValues(
            String metric, @Nullable String unit, RepoMetricMetadata metadata, float first, float second) {

        // majorAnyDelta, minorAnyDelta
        if (first != second && (metadata.majorAnyDelta() || metadata.minorAnyDelta()))
            return msg(
                    metadata.majorAnyDelta(),
                    metadata.minorAnyDelta(),
                    new MessageBuilder().addMetric(metric).addText(" changed.").build());

        // majorDeltaAmount, minorDeltaAmount
        float deltaAmount = second - first;
        boolean majorDeltaAmount = Math.abs(deltaAmount) > metadata.majorDeltaAmount();
        boolean minorDeltaAmount = Math.abs(deltaAmount) > metadata.minorDeltaAmount();
        if (majorDeltaAmount || minorDeltaAmount)
            return msg(
                    majorDeltaAmount,
                    minorDeltaAmount,
                    new MessageBuilder()
                            .addMetric(metric)
                            .addText(" changed by ")
                            .addDelta(deltaAmount, unit)
                            .addText(".")
                            .build());

        // majorDeltaFactor, minorDeltaFactor
        if (first != 0) {
            float deltaFactor = (second - first) / first;
            boolean majorDeltaFactor = Math.abs(deltaFactor) > metadata.majorDeltaFactor();
            boolean minorDeltaFactor = Math.abs(deltaFactor) > metadata.minorDeltaFactor();
            if (majorDeltaFactor || minorDeltaFactor)
                return msg(
                        majorDeltaFactor,
                        minorDeltaFactor,
                        new MessageBuilder()
                                .addMetric(metric)
                                .addText(" changed by ")
                                .addDeltaPercent(deltaFactor)
                                .addText(".")
                                .build());
        }

        return Optional.empty();
    }

    private static Optional<MetricMessage> msg(boolean major, boolean minor, List<JsonMessageSegment> message) {
        if (major) return Optional.of(new MetricMessage(MetricSignificance.Major, message));
        if (minor) return Optional.of(new MetricMessage(MetricSignificance.Minor, message));
        return Optional.empty();
    }
}
