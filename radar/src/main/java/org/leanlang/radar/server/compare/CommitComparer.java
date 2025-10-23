package org.leanlang.radar.server.compare;

import static org.leanlang.radar.codegen.jooq.Tables.MEASUREMENTS;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jooq.Configuration;
import org.jooq.Result;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.codegen.jooq.Tables;
import org.leanlang.radar.codegen.jooq.tables.records.MeasurementsRecord;
import org.leanlang.radar.codegen.jooq.tables.records.MetricsRecord;
import org.leanlang.radar.server.repos.Repo;
import org.leanlang.radar.server.repos.RepoMetricMetadata;

public final class CommitComparer {
    private CommitComparer() {}

    private record Measurement(float value, Optional<String> source) {}

    public static List<JsonMetricComparison> compareCommits(
            Repo repo, @Nullable String chashFirst, @Nullable String chashSecond) {
        return repo.db().readTransactionResult(ctx -> {
            Map<String, Measurement> measurementsFirst = measurementsFor(ctx, chashFirst);
            Map<String, Measurement> measurementsSecond = measurementsFor(ctx, chashSecond);

            List<JsonMetricComparison> result = new ArrayList<>();
            Result<MetricsRecord> metrics = ctx.dsl()
                    .selectFrom(Tables.METRICS)
                    .orderBy(Tables.METRICS.METRIC.asc())
                    .fetch();
            for (MetricsRecord row : metrics) {
                String metric = row.getMetric();
                Optional<String> unit = Optional.ofNullable(row.getUnit());
                RepoMetricMetadata metadata = repo.metricMetadata(metric);

                Optional<Measurement> first = Optional.ofNullable(measurementsFirst.get(metric));
                Optional<Measurement> second = Optional.ofNullable(measurementsSecond.get(metric));
                if (first.isEmpty() && second.isEmpty()) continue;

                Optional<Float> firstVal = first.map(Measurement::value);
                Optional<Float> secondVal = second.map(Measurement::value);
                Optional<String> firstSrc = first.flatMap(Measurement::source);
                Optional<String> secondSrc = second.flatMap(Measurement::source);

                Optional<JsonMetricSignificance> significance = SignificanceComputer.compareMetric(
                                metric, unit.orElse(null), metadata, firstVal.orElse(null), secondVal.orElse(null))
                        .map(it -> new JsonMetricSignificance(
                                it.significance().equals(MetricSignificance.Major), it.message()));

                result.add(new JsonMetricComparison(
                        metric, firstVal, secondVal, firstSrc, secondSrc, unit, metadata.direction(), significance));
            }

            return result;
        });
    }

    private static Map<String, Measurement> measurementsFor(Configuration ctx, @Nullable String chash) {
        if (chash == null) return Map.of();
        return ctx.dsl().selectFrom(MEASUREMENTS).where(MEASUREMENTS.CHASH.eq(chash)).stream()
                .collect(Collectors.toMap(
                        MeasurementsRecord::getMetric,
                        it -> new Measurement(it.getValue(), Optional.ofNullable(it.getSource()))));
    }
}
