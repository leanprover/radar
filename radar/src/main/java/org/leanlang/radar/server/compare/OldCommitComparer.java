package org.leanlang.radar.server.compare;

import static org.leanlang.radar.codegen.jooq.Tables.MEASUREMENTS;
import static org.leanlang.radar.codegen.jooq.Tables.QUEUE;
import static org.leanlang.radar.codegen.jooq.Tables.RUNS;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jooq.Configuration;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.codegen.jooq.Tables;
import org.leanlang.radar.codegen.jooq.tables.records.MeasurementsRecord;
import org.leanlang.radar.codegen.jooq.tables.records.MetricsRecord;
import org.leanlang.radar.codegen.jooq.tables.records.RunsRecord;
import org.leanlang.radar.server.repos.Repo;
import org.leanlang.radar.server.repos.RepoMetricMetadata;

// TODO Remove once old significance computation is obsolete
public final class OldCommitComparer {
    private OldCommitComparer() {}

    public static JsonCommitComparison compareCommits(
            Repo repo, @Nullable String chashFirst, @Nullable String chashSecond) {
        return repo.db().readTransactionResult(ctx -> compareCommits(repo, ctx, chashFirst, chashSecond));
    }

    public static JsonCommitComparison compareCommits(
            Repo repo, Configuration ctx, @Nullable String chashFirst, @Nullable String chashSecond) {

        List<MetricsRecord> metrics = fetchMetrics(ctx);
        Map<String, MeasurementsRecord> measurementsFirst = fetchMeasurements(ctx, chashFirst);
        Map<String, MeasurementsRecord> measurementsSecond = fetchMeasurements(ctx, chashSecond);
        List<RunsRecord> runsFirst = fetchRuns(ctx, chashFirst);
        List<RunsRecord> runsSecond = fetchRuns(ctx, chashSecond);
        boolean firstInQueue = fetchInQueue(ctx, chashFirst);
        boolean secondInQueue = fetchInQueue(ctx, chashSecond);

        List<JsonRunAnalysis> runAnalyses = analyzeRuns(runsSecond, !repo.significantRunFailures());
        List<JsonMetricComparison> measurementComparisons =
                compareMeasurements(repo, metrics, measurementsFirst, measurementsSecond, firstInQueue, secondInQueue);
        List<String> warnings = CommitComparer.findWarnings(runsFirst, runsSecond);

        JsonCommitComparison comparison =
                new JsonCommitComparison(false, runAnalyses, measurementComparisons, warnings);
        long significantRuns = comparison.runSignificances().count();
        long significantMajorMetrics = comparison
                .metricSignificances()
                .filter(it -> it.importance() >= JsonSignificance.IMPORTANCE_LARGE)
                .count();
        long significantMinorMetrics = comparison
                .metricSignificances()
                .filter(it -> it.importance() < JsonSignificance.IMPORTANCE_LARGE)
                .count();
        boolean significant = (significantRuns > 0)
                || (significantMajorMetrics >= repo.oldSignificantMajorMetrics())
                || (significantMinorMetrics >= repo.oldSignificantMinorMetrics());

        return new JsonCommitComparison(significant, comparison.runs(), comparison.metrics(), comparison.warnings());
    }

    private static List<MetricsRecord> fetchMetrics(Configuration ctx) {
        return ctx.dsl()
                .selectFrom(Tables.METRICS)
                .orderBy(Tables.METRICS.METRIC.asc())
                .fetch();
    }

    private static Map<String, MeasurementsRecord> fetchMeasurements(Configuration ctx, @Nullable String chash) {
        if (chash == null) return Map.of();
        return ctx.dsl().selectFrom(MEASUREMENTS).where(MEASUREMENTS.CHASH.eq(chash)).stream()
                .collect(Collectors.toMap(MeasurementsRecord::getMetric, it -> it));
    }

    private static List<RunsRecord> fetchRuns(Configuration ctx, @Nullable String chash) {
        if (chash == null) return List.of();
        return ctx.dsl().selectFrom(RUNS).where(RUNS.CHASH.eq(chash)).fetch();
    }

    private static boolean fetchInQueue(Configuration ctx, @Nullable String chash) {
        if (chash == null) return false;
        return ctx.dsl().fetchExists(QUEUE, QUEUE.CHASH.eq(chash));
    }

    private static List<JsonRunAnalysis> analyzeRuns(List<RunsRecord> runs, boolean ignoreFailedRuns) {
        return runs.stream()
                .map(run -> new JsonRunAnalysis(
                        run.getName(),
                        run.getScript(),
                        run.getRunner(),
                        run.getExitCode(),
                        analyzeRun(run.getName(), run.getExitCode(), ignoreFailedRuns)))
                .toList();
    }

    public static Optional<JsonSignificance> analyzeRun(String name, int exitCode, boolean ignoreFailedRuns) {
        if (ignoreFailedRuns) return Optional.empty();
        if (exitCode == 0) return Optional.empty();
        return msg(
                true,
                false,
                new OldMessageBuilder(OldMessageGoodness.BAD)
                        .addRun(name)
                        .addText(" exited with code ")
                        .addExitCode(exitCode)
                        .build());
    }

    private static String baseMetric(String metric, String baseCategory) {
        return ParsedMetric.parse(metric).withCategory(baseCategory).format();
    }

    private static List<JsonMetricComparison> compareMeasurements(
            Repo repo,
            List<MetricsRecord> metrics,
            Map<String, MeasurementsRecord> measurementsFirst,
            Map<String, MeasurementsRecord> measurementsSecond,
            boolean ignoreAppearances,
            boolean ignoreDisappearances) {

        List<JsonMetricComparison> result = new ArrayList<>();
        for (MetricsRecord metricRecord : metrics) {
            String metric = metricRecord.getMetric();
            Optional<String> unit = Optional.ofNullable(metricRecord.getUnit());
            RepoMetricMetadata metadata = repo.oldMetricMetadata(metric);
            Optional<MeasurementsRecord> firstRecord = Optional.ofNullable(measurementsFirst.get(metric));
            Optional<MeasurementsRecord> secondRecord = Optional.ofNullable(measurementsSecond.get(metric));
            if (firstRecord.isEmpty() && secondRecord.isEmpty()) continue;
            Optional<Float> first = firstRecord.map(MeasurementsRecord::getValue);
            Optional<Float> second = secondRecord.map(MeasurementsRecord::getValue);
            Optional<String> firstSrc = firstRecord.flatMap(it -> Optional.ofNullable(it.getSource()));
            Optional<String> secondSrc = secondRecord.flatMap(it -> Optional.ofNullable(it.getSource()));

            // Base metric
            Optional<String> baseMetric = metadata.baseCategory().map(it -> baseMetric(metric, it));
            Optional<Float> baseFirst = Optional.empty();
            Optional<Float> baseSecond = Optional.empty();
            if (baseMetric.isPresent()) {
                baseFirst = Optional.ofNullable(measurementsFirst.get(baseMetric.get()))
                        .map(MeasurementsRecord::getValue);
                baseSecond = Optional.ofNullable(measurementsSecond.get(baseMetric.get()))
                        .map(MeasurementsRecord::getValue);
            }

            Optional<JsonSignificance> significance = compareMetric(
                    metadata,
                    metric,
                    unit.orElse(null),
                    first.orElse(null),
                    second.orElse(null),
                    baseFirst.orElse(null),
                    baseSecond.orElse(null),
                    ignoreAppearances,
                    ignoreDisappearances);

            result.add(new JsonMetricComparison(
                    metric, first, second, firstSrc, secondSrc, unit, metadata.direction(), significance));
        }

        return result;
    }

    public static Optional<JsonSignificance> compareMetric(
            RepoMetricMetadata metadata,
            String metric,
            @Nullable String unit,
            @Nullable Float first,
            @Nullable Float second,
            @Nullable Float baseFirst,
            @Nullable Float baseSecond,
            boolean ignoreAppearances,
            boolean ignoreDisappearances) {

        if (first == null && second == null)
            throw new IllegalArgumentException("first and second must not both be null");

        // majorAppear, minorAppear
        if (first == null) {
            if (ignoreAppearances) return Optional.empty();
            return msg(
                    metadata.majorAppear(),
                    metadata.minorAppear(),
                    new OldMessageBuilder(OldMessageGoodness.NEUTRAL)
                            .addMetric(metric)
                            .addText(": appeared")
                            .build());
        }

        // majorDisappear, minorDisappear
        if (second == null) {
            if (ignoreDisappearances) return Optional.empty();
            return msg(
                    metadata.majorDisappear(),
                    metadata.minorDisappear(),
                    new OldMessageBuilder(OldMessageGoodness.NEUTRAL)
                            .addMetric(metric)
                            .addText(": disappeared")
                            .build());
        }

        // lowerThresholdAmount, upperThresholdAmount
        if (second < metadata.lowerThreshold() || second > metadata.upperThreshold()) {
            return Optional.empty();
        }

        // majorAnyDelta, minorAnyDelta
        if (!first.equals(second) && (metadata.majorAnyDelta() || metadata.minorAnyDelta()))
            return msg(
                    metadata.majorAnyDelta(),
                    metadata.minorAnyDelta(),
                    new OldMessageBuilder(OldMessageGoodness.fromDelta(second - first, metadata.direction()))
                            .addMetric(metric)
                            .addText(": ")
                            .addDeltaAndDeltaPercent(first, second, unit, metadata.direction())
                            .build());

        if (baseFirst != null && baseSecond != null) {
            return compareMetricWithValuesAndBase(metadata, metric, unit, first, second, baseFirst, baseSecond);
        } else {
            return compareMetricWithValues(metadata, metric, unit, first, second);
        }
    }

    private static Optional<JsonSignificance> compareMetricWithValues(
            RepoMetricMetadata metadata, String metric, @Nullable String unit, float first, float second) {

        // majorDeltaAmount, minorDeltaAmount
        float deltaAmount = second - first;
        boolean majorDeltaAmount = Math.abs(deltaAmount) > metadata.majorDeltaAmount();
        boolean minorDeltaAmount = Math.abs(deltaAmount) > metadata.minorDeltaAmount();
        if (majorDeltaAmount || minorDeltaAmount)
            return msg(
                    majorDeltaAmount,
                    minorDeltaAmount,
                    new OldMessageBuilder(OldMessageGoodness.fromDelta(deltaAmount, metadata.direction()))
                            .addMetric(metric)
                            .addText(": ")
                            .addDeltaAndDeltaPercent(first, second, unit, metadata.direction())
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
                        new OldMessageBuilder(OldMessageGoodness.fromDelta(deltaFactor, metadata.direction()))
                                .addMetric(metric)
                                .addText(": ")
                                .addDeltaAndDeltaPercent(first, second, unit, metadata.direction())
                                .build());
        }

        return Optional.empty();
    }

    private static Optional<JsonSignificance> compareMetricWithValuesAndBase(
            RepoMetricMetadata metadata,
            String metric,
            @Nullable String unit,
            float first,
            float second,
            float baseFirst,
            float baseSecond) {

        if (baseFirst == 0 || baseFirst == baseSecond)
            return compareMetricWithValues(metadata, metric, unit, first, second);
        float expectedSecond = first / baseFirst * baseSecond;

        // majorDeltaAmount, minorDeltaAmount
        float deltaAmountFromExpected = second - expectedSecond;
        boolean majorDeltaAmount = Math.abs(deltaAmountFromExpected) > metadata.majorDeltaAmount();
        boolean minorDeltaAmount = Math.abs(deltaAmountFromExpected) > metadata.minorDeltaAmount();
        if (majorDeltaAmount || minorDeltaAmount)
            return msg(
                    majorDeltaAmount,
                    minorDeltaAmount,
                    new OldMessageBuilder(OldMessageGoodness.fromDelta(deltaAmountFromExpected, metadata.direction()))
                            .addMetric(metric)
                            .addText(": ")
                            .addDeltaAndDeltaPercent(expectedSecond, second, unit, metadata.direction())
                            .addText(" from estimate, ")
                            .addDeltaAndDeltaPercent(first, second, unit, metadata.direction())
                            .addText(" from previous value")
                            .build());

        // majorDeltaFactor, minorDeltaFactor
        if (first != 0 && expectedSecond != 0) {
            float deltaFactorFromExpected = (second - expectedSecond) / expectedSecond;
            boolean majorDeltaFactor = Math.abs(deltaFactorFromExpected) > metadata.majorDeltaFactor();
            boolean minorDeltaFactor = Math.abs(deltaFactorFromExpected) > metadata.minorDeltaFactor();
            if (majorDeltaFactor || minorDeltaFactor)
                return msg(
                        majorDeltaFactor,
                        minorDeltaFactor,
                        new OldMessageBuilder(
                                        OldMessageGoodness.fromDelta(deltaAmountFromExpected, metadata.direction()))
                                .addMetric(metric)
                                .addText(": ")
                                .addDeltaAndDeltaPercent(expectedSecond, second, unit, metadata.direction())
                                .addText(" from estimate, ")
                                .addDeltaAndDeltaPercent(first, second, unit, metadata.direction())
                                .addText(" from previous value")
                                .build());
        }

        return Optional.empty();
    }

    private static Optional<JsonSignificance> msg(boolean major, boolean minor, OldJsonMessage message) {
        int goodness = message.goodness().toInt();
        List<JsonMessageSegment> segments = message.segments();
        if (major) return Optional.of(new JsonSignificance(JsonSignificance.IMPORTANCE_LARGE, goodness, segments));
        if (minor) return Optional.of(new JsonSignificance(JsonSignificance.IMPORTANCE_MEDIUM, goodness, segments));
        return Optional.empty();
    }
}
