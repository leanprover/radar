package org.leanlang.radar.server.api;

import static org.leanlang.radar.codegen.jooq.Tables.HISTORY;
import static org.leanlang.radar.codegen.jooq.Tables.MEASUREMENTS;
import static org.leanlang.radar.codegen.jooq.Tables.METRICS;
import static org.leanlang.radar.codegen.jooq.Tables.QUEUE;
import static org.leanlang.radar.codegen.jooq.Tables.RUNS;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.impl.DSL;
import org.leanlang.radar.server.repos.Repo;
import org.leanlang.radar.server.repos.Repos;

@Path("/admin/repos/{repo}/metrics/")
public record ResAdminRepoMetrics(Repos repos) {
    public record JsonMetric(
            @JsonProperty(required = true) String metric,
            Optional<String> unit,
            boolean appearsInLatestCommit,
            int appearsInHistoricalCommits) {}

    public record JsonGet(@JsonProperty(required = true) List<JsonMetric> metrics) {}

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonGet get(@PathParam("repo") String repoName) {
        Repo repo = repos.repo(repoName);

        return repo.db()
                .readTransactionResult(ctx -> {
                    String latestChash = ctx.dsl()
                            .selectFrom(HISTORY)
                            .whereExists(DSL.selectOne().from(RUNS).where(RUNS.CHASH.eq(HISTORY.CHASH)))
                            .andNotExists(DSL.selectOne().from(QUEUE).where(QUEUE.CHASH.eq(HISTORY.CHASH)))
                            .orderBy(HISTORY.POSITION.desc())
                            .limit(1)
                            .fetchOne(HISTORY.CHASH);

                    Set<String> latestMetrics;
                    if (latestChash == null) {
                        latestMetrics = Set.of();
                    } else {
                        latestMetrics = ctx
                                .dsl()
                                .select(MEASUREMENTS.METRIC)
                                .from(MEASUREMENTS)
                                .where(MEASUREMENTS.CHASH.eq(latestChash))
                                .stream()
                                .map(Record1::value1)
                                .collect(Collectors.toUnmodifiableSet());
                    }

                    Map<String, Integer> metricCounts = ctx
                            .dsl()
                            .select(MEASUREMENTS.METRIC, DSL.count())
                            .from(HISTORY.join(MEASUREMENTS).on(MEASUREMENTS.CHASH.eq(HISTORY.CHASH)))
                            .groupBy(MEASUREMENTS.METRIC)
                            .stream()
                            .collect(Collectors.toMap(Record2::value1, Record2::value2));

                    List<JsonMetric> metrics = ctx.dsl().selectFrom(METRICS).stream()
                            .map(it -> new JsonMetric(
                                    it.getMetric(),
                                    Optional.ofNullable(it.getUnit()),
                                    latestMetrics.contains(it.getMetric()),
                                    metricCounts.getOrDefault(it.getMetric(), 0)))
                            .toList();

                    return new JsonGet(metrics);
                });
    }
}
