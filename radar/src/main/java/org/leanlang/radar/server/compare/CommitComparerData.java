package org.leanlang.radar.server.compare;

import static org.leanlang.radar.codegen.jooq.Tables.MEASUREMENTS;
import static org.leanlang.radar.codegen.jooq.Tables.METRICS;
import static org.leanlang.radar.codegen.jooq.Tables.QUANTILE;
import static org.leanlang.radar.codegen.jooq.Tables.RUNS;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jooq.Configuration;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.codegen.jooq.tables.records.MeasurementsRecord;
import org.leanlang.radar.codegen.jooq.tables.records.QuantileRecord;
import org.leanlang.radar.codegen.jooq.tables.records.RunsRecord;
import org.leanlang.radar.server.config.ServerConfigRepoMetricFilter;
import org.leanlang.radar.server.queue.Queue;
import org.leanlang.radar.server.repos.Repo;

public record CommitComparerData(
        List<RunsRecord> runsFirst,
        List<RunsRecord> runsSecond,
        boolean enqueuedFirst,
        boolean enqueuedSecond,

        List<MetricInfo> metrics,
        List<String> notableMetrics,
        Set<String> notableMetricsSet,

        int significantLargeChanges,
        int significantMediumChanges,
        int significantSmallChanges,
        boolean significantRunFailures,

        Map<String, ServerConfigRepoMetricFilter> metricFilters,
        Map<String, JsonMetricComparison> metricComparisons) {

    public static CommitComparerData load(
            Queue queue, Repo repo, Repo quantileRepo, @Nullable String chashFirst, @Nullable String chashSecond) {

        Map<String, Float> quantiles = fetchQuantiles(quantileRepo);

        return repo.db().readTransactionResult(ctx -> {
            List<RunsRecord> runsFirst = fetchRuns(ctx, chashFirst);
            List<RunsRecord> runsSecond = fetchRuns(ctx, chashSecond);
            boolean enqueuedFirst = fetchInQueue(queue, repo, chashFirst);
            boolean enqueuedSecond = fetchInQueue(queue, repo, chashSecond);

            List<MetricInfo> metrics = fetchMetrics(ctx, quantiles);
            List<String> notableMetrics = repo.notableMetrics();
            Set<String> notableMetricsSet = repo.notableMetrics().stream().collect(Collectors.toUnmodifiableSet());

            int significantLargeChanges = repo.significantLargeChanges();
            int significantMediumChanges = repo.significantMediumChanges();
            int significantSmallChanges = repo.significantSmallChanges();
            boolean significantRunFailures = repo.significantRunFailures();

            Map<String, ServerConfigRepoMetricFilter> metricFilters =
                    metrics.stream().collect(Collectors.toMap(MetricInfo::name, it -> repo.metricFilter(it.name())));

            Map<String, MeasurementsRecord> measurementsFirst = fetchMeasurements(ctx, chashFirst);
            Map<String, MeasurementsRecord> measurementsSecond = fetchMeasurements(ctx, chashSecond);
            Map<String, JsonMetricComparison> metricComparisons =
                    compareMeasurements(metrics, metricFilters, measurementsFirst, measurementsSecond);

            return new CommitComparerData(
                    runsFirst,
                    runsSecond,
                    enqueuedFirst,
                    enqueuedSecond,
                    metrics,
                    notableMetrics,
                    notableMetricsSet,
                    significantLargeChanges,
                    significantMediumChanges,
                    significantSmallChanges,
                    significantRunFailures,
                    metricFilters,
                    metricComparisons);
        });
    }

    private static List<RunsRecord> fetchRuns(Configuration ctx, @Nullable String chash) {
        if (chash == null) return List.of();
        return ctx.dsl().selectFrom(RUNS).where(RUNS.CHASH.eq(chash)).fetch();
    }

    private static boolean fetchInQueue(Queue queue, Repo repo, @Nullable String chash) {
        if (chash == null) return false;
        return queue.isEnqueued(repo.name(), chash);
    }

    private static Map<String, Float> fetchQuantiles(Repo quantileRepo) {
        return quantileRepo.db().read().dsl().selectFrom(QUANTILE).stream()
                .collect(Collectors.toMap(QuantileRecord::getMetric, QuantileRecord::getValue));
    }

    private static List<MetricInfo> fetchMetrics(Configuration ctx, Map<String, Float> quantiles) {
        return ctx.dsl().selectFrom(METRICS).orderBy(METRICS.METRIC.asc()).stream()
                .map(it -> new MetricInfo(
                        it.getMetric(),
                        Optional.ofNullable(it.getUnit()),
                        Optional.ofNullable(quantiles.get(it.getMetric()))))
                .toList();
    }

    private static Map<String, MeasurementsRecord> fetchMeasurements(Configuration ctx, @Nullable String chash) {
        if (chash == null) return Map.of();
        return ctx.dsl().selectFrom(MEASUREMENTS).where(MEASUREMENTS.CHASH.eq(chash)).stream()
                .collect(Collectors.toMap(MeasurementsRecord::getMetric, it -> it));
    }

    private static Map<String, JsonMetricComparison> compareMeasurements(
            List<MetricInfo> metrics,
            Map<String, ServerConfigRepoMetricFilter> metricFilters,
            Map<String, MeasurementsRecord> measurementsFirst,
            Map<String, MeasurementsRecord> measurementsSecond) {

        Map<String, JsonMetricComparison> result = new HashMap<>();
        for (MetricInfo metric : metrics) {
            Optional<MeasurementsRecord> first = Optional.ofNullable(measurementsFirst.get(metric.name()));
            Optional<MeasurementsRecord> second = Optional.ofNullable(measurementsSecond.get(metric.name()));
            if (first.isEmpty() && second.isEmpty()) continue;
            JsonMetricComparison comparison = new JsonMetricComparison(
                    metric.name(),
                    first.map(MeasurementsRecord::getValue),
                    second.map(MeasurementsRecord::getValue),
                    first.map(MeasurementsRecord::getSource),
                    second.map(MeasurementsRecord::getSource),
                    metric.unit(),
                    metricFilters.get(metric.name()).direction);
            result.put(comparison.metric(), comparison);
        }
        return result;
    }
}
