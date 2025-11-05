package org.leanlang.radar.server.compare;

import static org.leanlang.radar.codegen.jooq.Tables.MEASUREMENTS;
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

public final class CommitComparer {
    private CommitComparer() {}

    public static JsonCommitComparison compareCommits(
            Repo repo, Configuration ctx, @Nullable String chashFirst, @Nullable String chashSecond) {

        List<MetricsRecord> metrics = fetchMetrics(ctx);
        Map<String, MeasurementsRecord> measurementsFirst = fetchMeasurements(ctx, chashFirst);
        Map<String, MeasurementsRecord> measurementsSecond = fetchMeasurements(ctx, chashSecond);
        List<RunsRecord> runsSecond = fetchRuns(ctx, chashSecond);

        List<JsonMetricComparison> measurementComparisons =
                compareMeasurements(repo, metrics, measurementsFirst, measurementsSecond);
        List<JsonRunAnalysis> runAnalyses = analyzeRuns(runsSecond);

        JsonCommitComparison comparison = new JsonCommitComparison(false, runAnalyses, measurementComparisons);
        long significantRuns = comparison.runSignificances().count();
        long significantMajorMetrics =
                comparison.metricSignificances().filter(JsonSignificance::major).count();
        long significantMinorMetrics =
                comparison.metricSignificances().filter(it -> !it.major()).count();
        boolean significant = (significantRuns > 0)
                || (significantMajorMetrics >= repo.significantMajorMetrics())
                || (significantMinorMetrics >= repo.significantMinorMetrics());

        return new JsonCommitComparison(significant, comparison.runs(), comparison.metrics());
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

    private static List<JsonMetricComparison> compareMeasurements(
            Repo repo,
            List<MetricsRecord> metrics,
            Map<String, MeasurementsRecord> measurementsFirst,
            Map<String, MeasurementsRecord> measurementsSecond) {

        List<JsonMetricComparison> result = new ArrayList<>();
        for (MetricsRecord metric : metrics) {
            String metricName = metric.getMetric();
            Optional<String> metricUnit = Optional.ofNullable(metric.getUnit());
            RepoMetricMetadata metricMetadata = repo.metricMetadata(metricName);

            Optional<MeasurementsRecord> first = Optional.ofNullable(measurementsFirst.get(metricName));
            Optional<MeasurementsRecord> second = Optional.ofNullable(measurementsSecond.get(metricName));
            if (first.isEmpty() && second.isEmpty()) continue;

            Optional<Float> firstVal = first.map(MeasurementsRecord::getValue);
            Optional<Float> secondVal = second.map(MeasurementsRecord::getValue);
            Optional<String> firstSrc = first.flatMap(it -> Optional.ofNullable(it.getSource()));
            Optional<String> secondSrc = second.flatMap(it -> Optional.ofNullable(it.getSource()));

            Optional<JsonSignificance> significance = SignificanceComputer.compareMetric(
                    metricName, metricUnit.orElse(null), metricMetadata, firstVal.orElse(null), secondVal.orElse(null));

            result.add(new JsonMetricComparison(
                    metricName,
                    firstVal,
                    secondVal,
                    firstSrc,
                    secondSrc,
                    metricUnit,
                    metricMetadata.direction(),
                    significance));
        }

        return result;
    }

    private static List<JsonRunAnalysis> analyzeRuns(List<RunsRecord> runs) {
        return runs.stream()
                .map(run -> new JsonRunAnalysis(
                        run.getName(),
                        run.getScript(),
                        run.getRunner(),
                        run.getExitCode(),
                        SignificanceComputer.analyzeRun(run.getName(), run.getExitCode())))
                .toList();
    }
}
