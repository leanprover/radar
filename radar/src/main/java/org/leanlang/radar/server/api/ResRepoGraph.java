package org.leanlang.radar.server.api;

import static org.leanlang.radar.codegen.jooq.Tables.COMMITS;
import static org.leanlang.radar.codegen.jooq.Tables.HISTORY;
import static org.leanlang.radar.codegen.jooq.Tables.MEASUREMENTS;
import static org.leanlang.radar.codegen.jooq.Tables.METRICS;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jooq.Record2;
import org.leanlang.radar.server.repos.Repo;
import org.leanlang.radar.server.repos.Repos;

@Path("/repos/{repo}/graph/")
public record ResRepoGraph(Repos repos) {
    public record JsonMetric(
            @JsonProperty(required = true) String metric,
            @JsonProperty(required = true) int direction,
            @JsonProperty(required = true) List<Float> measurements) {}

    public record JsonGet(
            @JsonProperty(required = true) List<JsonCommit> commits,
            @JsonProperty(required = true) List<JsonMetric> metrics) {}

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonGet get(@PathParam("repo") String name, @QueryParam("m") List<String> metrics, @QueryParam("n") int n) {
        Repo repo = repos.repo(name);
        if (metrics.size() > 10) throw new BadRequestException("too many metrics");
        if (n > 1000) throw new BadRequestException("n too large");

        return repo.db().readTransactionResult(ctx -> {
            List<JsonCommit> commits =
                    ctx
                            .dsl()
                            .selectFrom(HISTORY.join(COMMITS).onKey())
                            .orderBy(HISTORY.POSITION.desc())
                            .limit(n)
                            .stream()
                            .map(it -> it.into(COMMITS))
                            .map(JsonCommit::new)
                            .toList()
                            .reversed();

            Map<String, Integer> directions = ctx
                    .dsl()
                    .select(METRICS.METRIC, METRICS.DIRECTION)
                    .from(METRICS)
                    .where(METRICS.METRIC.in(metrics))
                    .stream()
                    .collect(Collectors.toUnmodifiableMap(Record2::value1, Record2::value2));

            List<JsonMetric> jsonMetrics = metrics.stream()
                    .sorted()
                    .map(it -> {
                        List<Float> measurements = ctx.dsl()
                                .select(MEASUREMENTS.VALUE)
                                .from(HISTORY.leftJoin(MEASUREMENTS).on(HISTORY.CHASH.eq(MEASUREMENTS.CHASH)))
                                .where(MEASUREMENTS.METRIC.eq(it))
                                .or(MEASUREMENTS.METRIC.isNull()) // Otherwise the left join doesn't work properly
                                .orderBy(HISTORY.POSITION.desc())
                                .limit(n)
                                .fetch(MEASUREMENTS.VALUE)
                                .reversed();

                        return new JsonMetric(it, directions.getOrDefault(it, 0), measurements);
                    })
                    .toList();

            return new JsonGet(commits, jsonMetrics);
        });
    }
}
