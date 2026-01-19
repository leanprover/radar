package org.leanlang.radar.server.compare;

import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.server.config.ServerConfigRepoMetricFilter;

public record MetricComparer(
        Map<String, JsonMetricComparison> metricComparisons,
        ServerConfigRepoMetricFilter metricFilter,
        MetricInfo metricInfo) {

    private @Nullable MetricComparisonSignificance checkDeltaPercent(float vFirst, float vSecond) {
        Float largeDelta = metricFilter.checkDeltaPercentLarge;
        Float mediumDelta = metricFilter.checkDeltaPercentMedium;
        Float smallDelta = metricFilter.checkDeltaPercentSmall;
        if (largeDelta == null && mediumDelta == null && smallDelta == null) return null;

        if (vFirst == 0) return null;
        float deltaPercent = (vSecond - vFirst) / vFirst * 100;
        float deltaPercentAbs = Math.abs(deltaPercent);

        boolean isLarge = largeDelta != null && deltaPercentAbs >= largeDelta;
        boolean isMedium = mediumDelta != null && deltaPercentAbs >= mediumDelta;
        boolean isSmall = smallDelta != null && deltaPercentAbs >= smallDelta;
        return MetricComparisonSignificance.fromLargeMediumSmall(isLarge, isMedium, isSmall);
    }

    private MetricComparisonSignificance checkQuantileFactor(float vFirst, float vSecond) {
        Float largeFactor = metricFilter.checkQuantileFactorLarge;
        Float mediumFactor = metricFilter.checkQuantileFactorMedium;
        Float smallFactor = metricFilter.checkQuantileFactorSmall;
        if (largeFactor == null && mediumFactor == null && smallFactor == null) return null;

        if (metricInfo.quantile().isEmpty()) return null;
        float qf = metricInfo.quantile().get();
        if (vFirst == vSecond) return null; // Prevent 0.0/0.0
        float f = Math.abs(vSecond - vFirst) / qf; // May be infinity

        boolean isLarge = largeFactor != null && f >= largeFactor;
        boolean isMedium = mediumFactor != null && f >= mediumFactor;
        boolean isSmall = smallFactor != null && f >= smallFactor;
        return MetricComparisonSignificance.fromLargeMediumSmall(isLarge, isMedium, isSmall);
    }

    private MetricComparisonSignificance reduceExpectedDirection(
            float vFirst, float vSecond, MetricComparisonSignificance significance, JsonMessageBuilder message) {

        String referenceCategory = metricFilter.reduceExpectedDirectionReferenceCategory;
        if (referenceCategory == null) return significance;

        String referenceMetric = ParsedMetric.parse(metricInfo.name())
                .withCategory(referenceCategory)
                .format();

        JsonMetricComparison referenceComparison = metricComparisons.get(referenceMetric);
        if (referenceComparison == null
                || referenceComparison.first().isEmpty()
                || referenceComparison.second().isEmpty()) return significance;
        float rvFirst = referenceComparison.first().get();
        float rvSecond = referenceComparison.second().get();

        float mDelta = vSecond - vFirst;
        float rDelta = rvSecond - rvFirst;
        boolean expected = (rDelta > 0 && mDelta > 0) || (rDelta < 0 && mDelta < 0);
        if (!expected) return significance;

        if (significance != MetricComparisonSignificance.SMALL)
            message.addText(" (reduced significance based on *//" + referenceCategory + ")");

        return MetricComparisonSignificance.SMALL;
    }

    private MetricComparisonSignificance reduceAbsoluteLimits(
            float vFirst, float vSecond, MetricComparisonSignificance significance, JsonMessageBuilder message) {

        Float smallLimit = metricFilter.reduceAbsoluteLimitsSmall;
        Float mediumLimit = metricFilter.reduceAbsoluteLimitsMedium;
        if (smallLimit == null && mediumLimit == null) return significance;

        float delta = Math.abs(vSecond - vFirst);

        boolean shouldBeSmall = smallLimit != null && delta < smallLimit;
        boolean shouldBeMedium = mediumLimit != null && delta < mediumLimit;

        if (shouldBeSmall && significance.compareTo(MetricComparisonSignificance.SMALL) > 0) {
            message.addText(" (reduced significance based on absolute threshold)");
            return MetricComparisonSignificance.SMALL;
        }

        if (shouldBeMedium && significance.compareTo(MetricComparisonSignificance.MEDIUM) > 0) {
            message.addText(" (reduced significance based on absolute threshold)");
            return MetricComparisonSignificance.MEDIUM;
        }

        return significance;
    }

    private Optional<MetricComparison> compare() {
        JsonMetricComparison comparison = metricComparisons.get(metricInfo.name());
        if (comparison == null) return Optional.empty();
        if (comparison.first().isEmpty() || comparison.second().isEmpty()) return Optional.empty();

        float vFirst = comparison.first().get();
        float vSecond = comparison.second().get();

        JsonMessageBuilder message = JsonMessageBuilder.metricDeltaDeltaPercentGoodness(
                metricInfo.name(), vFirst, vSecond, comparison.unit().orElse(null), metricFilter.direction);

        MetricComparisonSignificance sigDelta = checkDeltaPercent(vFirst, vSecond);
        MetricComparisonSignificance sigQuantile = checkQuantileFactor(vFirst, vSecond);
        MetricComparisonSignificance significance = MetricComparisonSignificance.maxNullable(sigDelta, sigQuantile);
        if (significance != null) significance = reduceExpectedDirection(vFirst, vSecond, significance, message);
        if (significance != null) significance = reduceAbsoluteLimits(vFirst, vSecond, significance, message);

        if (significance == null) return Optional.empty();
        return Optional.of(new MetricComparison(significance, message.build()));
    }

    public static Optional<MetricComparison> compare(
            Map<String, JsonMetricComparison> metricComparisons,
            ServerConfigRepoMetricFilter metricFilter,
            MetricInfo metricInfo) {
        return new MetricComparer(metricComparisons, metricFilter, metricInfo).compare();
    }
}
