package org.leanlang.radar.server.api;

import static org.leanlang.radar.codegen.jooq.Tables.COMMITS;
import static org.leanlang.radar.codegen.jooq.Tables.HISTORY;
import static org.leanlang.radar.codegen.jooq.Tables.MEASUREMENTS;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.jooq.impl.DSL;
import org.leanlang.radar.server.repos.Repo;
import org.leanlang.radar.server.repos.Repos;

@Path("/repos/{repo}/graph/")
public record ResRepoGraph(Repos repos) {

    public static final int METRICS_LIMIT = 500;
    public static final int COMMITS_LIMIT = 100000;

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
        if (metrics.size() > METRICS_LIMIT) throw new BadRequestException("too many metrics");
        if (n > COMMITS_LIMIT) throw new BadRequestException("n too large");

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

            List<JsonMetric> jsonMetrics = metrics.stream()
                    .sorted()
                    .map(it -> {
                        List<Float> measurements = ctx.dsl()
                                .select(HISTORY.POSITION, MEASUREMENTS.VALUE)
                                .from(HISTORY.naturalJoin(MEASUREMENTS))
                                .where(MEASUREMENTS.METRIC.eq(it))
                                .union(DSL.select(HISTORY.POSITION, DSL.inline((Float) null))
                                        .from(HISTORY)
                                        .whereNotExists(DSL.selectOne()
                                                .from(MEASUREMENTS)
                                                .where(MEASUREMENTS.CHASH.eq(HISTORY.CHASH))
                                                .and(MEASUREMENTS.METRIC.eq(it))))
                                .orderBy(HISTORY.POSITION.desc())
                                .limit(n)
                                .fetch(MEASUREMENTS.VALUE)
                                .reversed();

                        return new JsonMetric(it, repo.metricMetadata(it).direction(), measurements);
                    })
                    .toList();

            return new JsonGet(commits, jsonMetrics);
        });
    }
}
