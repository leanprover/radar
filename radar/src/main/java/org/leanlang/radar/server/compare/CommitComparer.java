package org.leanlang.radar.server.compare;

import static org.leanlang.radar.codegen.jooq.Tables.MEASUREMENTS;
import static org.leanlang.radar.codegen.jooq.Tables.METRICS;
import static org.leanlang.radar.codegen.jooq.Tables.QUANTILE;
import static org.leanlang.radar.codegen.jooq.Tables.RUNS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jooq.Configuration;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.codegen.jooq.tables.records.MeasurementsRecord;
import org.leanlang.radar.codegen.jooq.tables.records.RunsRecord;
import org.leanlang.radar.server.config.ServerConfigRepoMetricFilter;
import org.leanlang.radar.server.queue.Queue;
import org.leanlang.radar.server.repos.Repo;

public final class CommitComparer {
    Repo repo;

    private final List<RunsRecord> runsFirst;
    private final List<RunsRecord> runsSecond;
    private final boolean enqueuedFirst;
    private final boolean enqueuedSecond;

    private final List<MetricInfo> metrics;
    private final Map<String, ServerConfigRepoMetricFilter> metricFilters;
    private final Map<String, JsonMetricComparison> metricComparisons;

    private final List<String> warnings = new ArrayList<>();
    private final List<JsonMessage> notes = new ArrayList<>();
    private final List<JsonMessage> fatalNotes = new ArrayList<>();
    private final List<JsonMessage> largeChanges = new ArrayList<>();
    private final List<JsonMessage> mediumChanges = new ArrayList<>();
    private final List<JsonMessage> smallChanges = new ArrayList<>();

    private CommitComparer(
            Queue queue, Repo repo, Configuration ctx, @Nullable String chashFirst, @Nullable String chashSecond) {

        this.repo = repo;

        runsFirst = fetchRuns(ctx, chashFirst);
        runsSecond = fetchRuns(ctx, chashSecond);
        enqueuedFirst = fetchInQueue(queue, repo, chashFirst);
        enqueuedSecond = fetchInQueue(queue, repo, chashSecond);

        metrics = fetchMetrics(ctx);
        metricFilters =
                metrics.stream().collect(Collectors.toMap(MetricInfo::name, it -> repo.metricFilter(it.name())));
        Map<String, MeasurementsRecord> measurementsFirst = fetchMeasurements(ctx, chashFirst);
        Map<String, MeasurementsRecord> measurementsSecond = fetchMeasurements(ctx, chashSecond);
        metricComparisons = compareMeasurements(metrics, metricFilters, measurementsFirst, measurementsSecond);

        analyzeRunsForWarnings();
        analyzeRunsForNotes();
        noteNotableMetrics();
        analyzeMeasurements();
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

    private static boolean fetchInQueue(Queue queue, Repo repo, @Nullable String chash) {
        if (chash == null) return false;
        return queue.isEnqueued(repo.name(), chash);
    }

    private static List<RunsRecord> fetchRuns(Configuration ctx, @Nullable String chash) {
        if (chash == null) return List.of();
        return ctx.dsl().selectFrom(RUNS).where(RUNS.CHASH.eq(chash)).fetch();
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

    private void analyzeRunsForWarnings() {
        if (enqueuedSecond) return;

        if (enqueuedFirst) {
            warnings.add("The reference commit has not finished benchmarking yet.");
            return;
        }

        Map<String, RunsRecord> byNameFirst =
                runsFirst.stream().collect(Collectors.toMap(RunsRecord::getName, it -> it));
        Map<String, RunsRecord> byNameSecond =
                runsSecond.stream().collect(Collectors.toMap(RunsRecord::getName, it -> it));

        Set<String> namesSet = new HashSet<>();
        namesSet.addAll(byNameFirst.keySet());
        namesSet.addAll(byNameSecond.keySet());

        List<String> names = namesSet.stream().sorted().toList();
        for (String name : names) {
            if (!byNameFirst.containsKey(name)) {
                warnings.add("Run " + name + " is only present in the second (current) commit.");
                continue;
            }

            if (!byNameSecond.containsKey(name)) {
                warnings.add("Run " + name + " is only present in the first (reference) commit.");
                continue;
            }

            RunsRecord runFirst = byNameFirst.get(name);
            RunsRecord runSecond = byNameSecond.get(name);
            if (!runFirst.getRunner().equals(runSecond.getRunner()))
                warnings.add("Runners for run " + name + " differ between commits.");
            if (!runFirst.getScript().equals(runSecond.getScript()))
                warnings.add("Scripts for run " + name + " differ between commits.");
            if (!runFirst.getChashBench().equals(runSecond.getChashBench()))
                warnings.add("Bench repo commit hashes for run " + name + " differ between commits.");
            if (!Objects.equals(runFirst.getSystemConfigurationId(), runSecond.getSystemConfigurationId()))
                warnings.add("Runner for run " + name + " has different system configurations between commits.");
        }
    }

    private void analyzeRunsForNotes() {
        if (!repo.significantRunFailures()) return;

        for (RunsRecord run : runsSecond) {
            if (run.getExitCode() == 0) continue;
            fatalNotes.add(new JsonMessageBuilder()
                    .setGoodness(JsonMessageGoodness.BAD)
                    .addRun(run.getName())
                    .addText(" exited with code ")
                    .addExitCode(run.getExitCode())
                    .build());
        }
    }

    private void noteNotableMetrics() {
        for (String metric : repo.notableMetrics()) {
            JsonMetricComparison comparison = metricComparisons.get(metric);
            if (comparison == null
                    || comparison.first().isEmpty()
                    || comparison.second().isEmpty()) continue;
            Float vFirst = comparison.first().get();
            Float vSecond = comparison.second().get();
            notes.add(JsonMessageBuilder.metricDeltaDeltaPercentGoodness(
                            metric,
                            vFirst,
                            vSecond,
                            comparison.unit().orElse(null),
                            metricFilters.get(metric).direction)
                    .build());
        }
    }

    private void analyzeMeasurements() {
        for (MetricInfo metric : metrics) {
            ServerConfigRepoMetricFilter metricFilter = metricFilters.get(metric.name());
            MetricComparison comparison = MetricComparer.compare(metricComparisons, metricFilter, metric)
                    .orElse(null);
            if (comparison == null) continue;

            switch (comparison.significance()) {
                case SMALL -> smallChanges.add(comparison.message());
                case MEDIUM -> mediumChanges.add(comparison.message());
                case LARGE -> largeChanges.add(comparison.message());
            }
        }
    }

    private JsonCommitComparison comparison() {
        int largeChangesAmount = largeChanges.size();
        int mediumChangesAmount = largeChangesAmount + mediumChanges.size();
        int smallChangesAmount = mediumChangesAmount + smallChanges.size();

        boolean significant = !fatalNotes.isEmpty()
                || largeChangesAmount >= repo.significantLargeChanges()
                || mediumChangesAmount >= repo.significantMediumChanges()
                || smallChangesAmount >= repo.significantSmallChanges();

        List<JsonMessage> allNotes =
                Stream.concat(notes.stream(), fatalNotes.stream()).toList();

        List<JsonMetricComparison> measurements = metrics.stream()
                .map(it -> metricComparisons.get(it.name()))
                .filter(Objects::nonNull)
                .toList();

        return new JsonCommitComparison(
                significant, warnings, allNotes, largeChanges, mediumChanges, smallChanges, measurements);
    }

    public static JsonCommitComparison compareCommits(
            Queue queue, Repo repo, @Nullable String chashFirst, @Nullable String chashSecond) {
        return repo.db().readTransactionResult(ctx -> compareCommits(queue, repo, ctx, chashFirst, chashSecond));
    }

    public static JsonCommitComparison compareCommits(
            Queue queue, Repo repo, Configuration ctx, @Nullable String chashFirst, @Nullable String chashSecond) {
        return new CommitComparer(queue, repo, ctx, chashFirst, chashSecond).comparison();
    }
}
