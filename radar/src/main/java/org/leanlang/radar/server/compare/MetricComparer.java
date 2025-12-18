package org.leanlang.radar.server.compare;

import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.codegen.jooq.tables.records.MeasurementsRecord;
import org.leanlang.radar.server.config.ServerConfigRepoMetricFilter;

public class MetricComparer {
    private final ServerConfigRepoMetricFilter metricFilter;
    private final MetricInfo metricInfo;
    private final Map<String, MeasurementsRecord> measurementsFirst;
    private final Map<String, MeasurementsRecord> measurementsSecond;

    private @Nullable SignificanceBuilder significance;

    private MetricComparer(
            ServerConfigRepoMetricFilter metricFilter,
            MetricInfo metricInfo,
            Map<String, MeasurementsRecord> measurementsFirst,
            Map<String, MeasurementsRecord> measurementsSecond) {
        this.metricFilter = metricFilter;
        this.metricInfo = metricInfo;
        this.measurementsFirst = measurementsFirst;
        this.measurementsSecond = measurementsSecond;
    }

    private String metric() {
        return metricInfo.name();
    }

    private @Nullable MeasurementsRecord firstMeasurement() {
        return measurementsFirst.get(metric());
    }

    private @Nullable MeasurementsRecord secondMeasurement() {
        return measurementsSecond.get(metric());
    }

    private SignificanceBuilder builderDelta(int importance, float vFirst, float vSecond) {
        return new SignificanceBuilder(importance)
                .addMetric(metric())
                .addText(": ")
                .addDeltaAndDeltaPercent(vFirst, vSecond, metricInfo.unit().orElse(null), metricFilter.direction);
    }

    private void checkDeltaPercent(float vFirst, float vSecond) {
        Float largeDelta = metricFilter.checkDeltaPercentLarge;
        Float mediumDelta = metricFilter.checkDeltaPercentMedium;
        Float smallDelta = metricFilter.checkDeltaPercentSmall;
        if (largeDelta == null && mediumDelta == null && smallDelta == null) return;

        if (vFirst == 0) return;
        float deltaPercent = (vSecond - vFirst) / vFirst * 100;
        float deltaPercentAbs = Math.abs(deltaPercent);

        boolean isLarge = largeDelta != null && deltaPercentAbs >= largeDelta;
        boolean isMedium = mediumDelta != null && deltaPercentAbs >= mediumDelta;
        boolean isSmall = smallDelta != null && deltaPercentAbs >= smallDelta;

        int importance;
        if (isLarge) importance = JsonSignificance.IMPORTANCE_LARGE;
        else if (isMedium) importance = JsonSignificance.IMPORTANCE_MEDIUM;
        else if (isSmall) importance = JsonSignificance.IMPORTANCE_SMALL;
        else return;

        significance = builderDelta(importance, vFirst, vSecond)
                .setGoodnessWithDirection(metricFilter.direction, vSecond - vFirst);
    }

    private void checkQuantileFactor(float vFirst, float vSecond) {
        Float largeFactor = metricFilter.checkQuantileFactorLarge;
        Float mediumFactor = metricFilter.checkQuantileFactorMedium;
        Float smallFactor = metricFilter.checkQuantileFactorSmall;
        if (largeFactor == null && mediumFactor == null && smallFactor == null) return;

        if (metricInfo.quantile().isEmpty()) return;
        float qf = metricInfo.quantile().get();
        if (vFirst == vSecond) return; // Prevent 0.0/0.0
        float f = Math.abs(vSecond - vFirst) / qf; // May be infinity

        boolean isLarge = largeFactor != null && f >= largeFactor;
        boolean isMedium = mediumFactor != null && f >= mediumFactor;
        boolean isSmall = smallFactor != null && f >= smallFactor;

        int importance;
        if (isLarge) importance = JsonSignificance.IMPORTANCE_LARGE;
        else if (isMedium) importance = JsonSignificance.IMPORTANCE_MEDIUM;
        else if (isSmall) importance = JsonSignificance.IMPORTANCE_SMALL;
        else return;

        significance = builderDelta(importance, vFirst, vSecond)
                .setGoodnessWithDirection(metricFilter.direction, vSecond - vFirst);
    }

    private void reduceExpectedDirection(float vFirst, float vSecond) {
        if (significance == null) return;

        String referenceCategory = metricFilter.reduceExpectedDirectionReferenceCategory;
        if (referenceCategory == null) return;

        String referenceMetric = ParsedMetric.parse(metricInfo.name())
                .withCategory(referenceCategory)
                .format();

        MeasurementsRecord rFirst = measurementsFirst.get(referenceMetric);
        MeasurementsRecord rSecond = measurementsSecond.get(referenceMetric);
        if (rFirst == null || rSecond == null) return;

        float mDelta = vSecond - vFirst;
        float rDelta = rSecond.getValue() - rFirst.getValue();
        boolean expected = (rDelta > 0 && mDelta > 0) || (rDelta < 0 && mDelta < 0);
        if (!expected) return;

        int importance = significance.importance();
        if (importance <= JsonSignificance.IMPORTANCE_SMALL) return;

        significance
                .setImportance(JsonSignificance.IMPORTANCE_SMALL)
                .addText(" (reduced significance based on *//" + referenceCategory + ")");
    }

    private void reduceAbsoluteLimits(float vFirst, float vSecond) {
        if (significance == null) return;

        Float smallLimit = metricFilter.reduceAbsoluteLimitsSmall;
        Float mediumLimit = metricFilter.reduceAbsoluteLimitsMedium;
        if (smallLimit == null && mediumLimit == null) return;

        float delta = Math.abs(vSecond - vFirst);

        boolean shouldBeSmall = smallLimit != null && delta < smallLimit;
        boolean shouldBeMedium = mediumLimit != null && delta < mediumLimit;

        int importance = significance.importance();
        if (shouldBeSmall && importance > JsonSignificance.IMPORTANCE_SMALL)
            significance
                    .setImportance(JsonSignificance.IMPORTANCE_SMALL)
                    .addText(" (reduced significance based on absolute threshold)");
        else if (shouldBeMedium && importance > JsonSignificance.IMPORTANCE_MEDIUM)
            significance
                    .setImportance(JsonSignificance.IMPORTANCE_MEDIUM)
                    .addText(" (reduced significance based on absolute threshold)");
    }

    private void compare() {
        MeasurementsRecord mFirst = firstMeasurement();
        MeasurementsRecord mSecond = secondMeasurement();
        if (mFirst == null || mSecond == null) return;
        float vFirst = mFirst.getValue();
        float vSecond = mSecond.getValue();

        checkDeltaPercent(vFirst, vSecond);
        checkQuantileFactor(vFirst, vSecond);
        reduceExpectedDirection(vFirst, vSecond);
        reduceAbsoluteLimits(vFirst, vSecond);
    }

    public static Optional<JsonSignificance> compare(
            ServerConfigRepoMetricFilter metricFilter,
            MetricInfo metricInfo,
            Map<String, MeasurementsRecord> measurementsFirst,
            Map<String, MeasurementsRecord> measurementsSecond) {

        MetricComparer comparer = new MetricComparer(metricFilter, metricInfo, measurementsFirst, measurementsSecond);
        comparer.compare();
        return Optional.ofNullable(comparer.significance).map(SignificanceBuilder::build);
    }
}
