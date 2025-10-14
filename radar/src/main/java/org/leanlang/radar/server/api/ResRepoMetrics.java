package org.leanlang.radar.server.api;

import static org.leanlang.radar.codegen.jooq.Tables.METRICS;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Optional;
import org.leanlang.radar.server.repos.Repo;
import org.leanlang.radar.server.repos.Repos;

@Path("/repos/{repo}/metrics/")
public record ResRepoMetrics(Repos repos) {
    public record JsonMetric(@JsonProperty(required = true) String metric, Optional<String> unit) {}

    public record JsonGet(@JsonProperty(required = true) List<JsonMetric> metrics) {}

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonGet get(@PathParam("repo") String repoName) {
        Repo repo = repos.repo(repoName);

        List<JsonMetric> metrics = repo.db().read().dsl().selectFrom(METRICS).orderBy(METRICS.METRIC).stream()
                .map(it -> new JsonMetric(it.getMetric(), Optional.ofNullable(it.getUnit())))
                .toList();

        return new JsonGet(metrics);
    }
}
