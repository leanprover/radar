package org.leanlang.radar.server.compare;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.codegen.jooq.tables.records.RunsRecord;
import org.leanlang.radar.server.config.ServerConfigRepoMetricFilter;
import org.leanlang.radar.server.queue.Queue;
import org.leanlang.radar.server.repos.Repo;
import org.leanlang.radar.server.repos.Repos;

public final class CommitComparer {
    CommitComparerData data;

    private final List<String> warnings = new ArrayList<>();
    private final List<JsonMessage> notes = new ArrayList<>();
    private final List<JsonMessage> fatalNotes = new ArrayList<>();
    private final List<JsonMessage> largeChanges = new ArrayList<>();
    private final List<JsonMessage> mediumChanges = new ArrayList<>();
    private final List<JsonMessage> smallChanges = new ArrayList<>();

    private CommitComparer(CommitComparerData data) {
        this.data = data;

        analyzeRunsForWarnings();
        analyzeRunsForNotes();
        noteNotableMetrics();
        analyzeMeasurements();
    }

    private void analyzeRunsForWarnings() {
        if (data.enqueuedSecond()) return;

        if (data.enqueuedFirst()) {
            warnings.add("The reference commit has not finished benchmarking yet.");
            return;
        }

        Map<String, RunsRecord> byNameFirst =
                data.runsFirst().stream().collect(Collectors.toMap(RunsRecord::getName, it -> it));
        Map<String, RunsRecord> byNameSecond =
                data.runsSecond().stream().collect(Collectors.toMap(RunsRecord::getName, it -> it));

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
        if (!data.significantRunFailures()) return;

        for (RunsRecord run : data.runsSecond()) {
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
        for (String metric : data.notableMetrics()) {
            JsonMetricComparison comparison = data.metricComparisons().get(metric);
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
                            data.metricFilters().get(metric).direction)
                    .build());
        }
    }

    private void analyzeMeasurements() {
        for (MetricInfo metric : data.metrics()) {
            ServerConfigRepoMetricFilter metricFilter = data.metricFilters().get(metric.name());
            MetricComparison comparison = MetricComparer.compare(data.metricComparisons(), metricFilter, metric)
                    .orElse(null);
            if (comparison == null) continue;

            final JsonMessage message = comparison
                    .message()
                    .setHidden(data.notableMetricsSet().contains(metric.name()))
                    .build();

            switch (comparison.significance()) {
                case SMALL -> smallChanges.add(message);
                case MEDIUM -> mediumChanges.add(message);
                case LARGE -> largeChanges.add(message);
            }
        }
    }

    private JsonCommitComparison comparison() {
        int largeChangesAmount = largeChanges.size();
        int mediumChangesAmount = largeChangesAmount + mediumChanges.size();
        int smallChangesAmount = mediumChangesAmount + smallChanges.size();

        boolean significant = !fatalNotes.isEmpty()
                || largeChangesAmount >= data.significantLargeChanges()
                || mediumChangesAmount >= data.significantMediumChanges()
                || smallChangesAmount >= data.significantSmallChanges();

        List<JsonMessage> allNotes =
                Stream.concat(notes.stream(), fatalNotes.stream()).toList();

        List<JsonMetricComparison> measurements = data.metrics().stream()
                .map(it -> data.metricComparisons().get(it.name()))
                .filter(Objects::nonNull)
                .toList();

        return new JsonCommitComparison(
                significant, warnings, allNotes, largeChanges, mediumChanges, smallChanges, measurements);
    }

    public static JsonCommitComparison compareCommits(
            Queue queue, Repos repos, Repo repo, @Nullable String chashFirst, @Nullable String chashSecond) {

        Repo quantileRepo = repo.useQuantilesFrom().map(repos::repo).orElse(repo);
        CommitComparerData data = CommitComparerData.load(queue, repo, quantileRepo, chashFirst, chashSecond);
        return new CommitComparer(data).comparison();
    }
}
