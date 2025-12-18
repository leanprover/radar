package org.leanlang.radar.server.compare;

import static org.leanlang.radar.codegen.jooq.Tables.MEASUREMENTS;
import static org.leanlang.radar.codegen.jooq.Tables.METRICS;
import static org.leanlang.radar.codegen.jooq.Tables.QUANTILE;
import static org.leanlang.radar.codegen.jooq.Tables.QUEUE;
import static org.leanlang.radar.codegen.jooq.Tables.RUNS;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jooq.Configuration;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.codegen.jooq.tables.records.MeasurementsRecord;
import org.leanlang.radar.codegen.jooq.tables.records.RunsRecord;
import org.leanlang.radar.server.config.ServerConfigRepoMetricFilter;
import org.leanlang.radar.server.repos.Repo;

public final class CommitComparer {
    private CommitComparer() {}

    public static JsonCommitComparison compareCommits(
            Repo repo, @Nullable String chashFirst, @Nullable String chashSecond) {
        return repo.db().readTransactionResult(ctx -> compareCommits(repo, ctx, chashFirst, chashSecond));
    }

    public static JsonCommitComparison compareCommits(
            Repo repo, Configuration ctx, @Nullable String chashFirst, @Nullable String chashSecond) {

        List<MetricInfo> metrics = fetchMetrics(ctx);
        Map<String, MeasurementsRecord> measurementsFirst = fetchMeasurements(ctx, chashFirst);
        Map<String, MeasurementsRecord> measurementsSecond = fetchMeasurements(ctx, chashSecond);
        List<RunsRecord> runsSecond = fetchRuns(ctx, chashSecond);

        // TODO Get rid of all old comparison logic
        if (repo.useOldSignificance()) return OldCommitComparer.compareCommits(repo, ctx, chashFirst, chashSecond);

        List<JsonRunAnalysis> runAnalyses = analyzeRuns(runsSecond, !repo.significantRunFailures());
        List<JsonMetricComparison> metricComparisons =
                compareMetrics(repo, metrics, measurementsFirst, measurementsSecond);

        // Large changes also count as medium and small changes.
        // Medium changes also count as small changes.
        int smallChanges = 0;
        int mediumChanges = 0;
        int largeChanges = 0;
        JsonCommitComparison tmpComparison = new JsonCommitComparison(false, runAnalyses, metricComparisons);
        for (JsonSignificance significance : tmpComparison.significances().toList()) {
            if (significance.importance() >= JsonSignificance.IMPORTANCE_SMALL) smallChanges++;
            if (significance.importance() >= JsonSignificance.IMPORTANCE_MEDIUM) mediumChanges++;
            if (significance.importance() >= JsonSignificance.IMPORTANCE_LARGE) largeChanges++;
        }

        boolean significant = (smallChanges >= repo.significantSmallChanges())
                || (mediumChanges >= repo.significantMediumChanges())
                || (largeChanges >= repo.significantLargeChanges());

        return new JsonCommitComparison(significant, runAnalyses, metricComparisons);
    }

    private static List<MetricInfo> fetchMetrics(Configuration ctx) {
        return ctx
                .dsl()
                .select(METRICS.METRIC, METRICS.UNIT, QUANTILE.VALUE)
                .from(METRICS.leftJoin(QUANTILE).onKey())
                .orderBy(METRICS.METRIC.asc())
                .stream()
                .map(it ->
                        new MetricInfo(it.value1(), Optional.ofNullable(it.value2()), Optional.ofNullable(it.value3())))
                .toList();
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
        return new SignificanceBuilder(JsonSignificance.IMPORTANCE_LARGE)
                .setGoodness(JsonSignificance.GOODNESS_BAD)
                .addRun(name)
                .addText(" exited with code ")
                .addExitCode(exitCode)
                .buildOpt();
    }

    private static List<JsonMetricComparison> compareMetrics(
            Repo repo,
            List<MetricInfo> metrics,
            Map<String, MeasurementsRecord> measurementsFirst,
            Map<String, MeasurementsRecord> measurementsSecond) {

        List<JsonMetricComparison> result = new ArrayList<>();
        for (MetricInfo metricInfo : metrics) {
            JsonMetricComparison comparison = compareMetric(repo, metricInfo, measurementsFirst, measurementsSecond);
            if (comparison == null) continue;
            result.add(comparison);
        }
        return result;
    }

    private static @Nullable JsonMetricComparison compareMetric(
            Repo repo,
            MetricInfo metricInfo,
            Map<String, MeasurementsRecord> measurementsFirst,
            Map<String, MeasurementsRecord> measurementsSecond) {

        String metric = metricInfo.name();
        ServerConfigRepoMetricFilter metricFilter = repo.metricFilter(metric);

        Optional<MeasurementsRecord> mFirst = Optional.ofNullable(measurementsFirst.get(metric));
        Optional<MeasurementsRecord> mSecond = Optional.ofNullable(measurementsSecond.get(metric));
        if (mFirst.isEmpty() && mSecond.isEmpty()) return null;

        Optional<JsonSignificance> significance = MetricComparer.compare(
                repo.metricFilter(metricInfo.name()), metricInfo, measurementsFirst, measurementsSecond);

        return new JsonMetricComparison(
                metric,
                mFirst.map(MeasurementsRecord::getValue),
                mSecond.map(MeasurementsRecord::getValue),
                mFirst.map(MeasurementsRecord::getSource),
                mSecond.map(MeasurementsRecord::getSource),
                metricInfo.unit(),
                metricFilter.direction,
                significance);
    }
}
