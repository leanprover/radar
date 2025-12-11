package org.leanlang.radar.server.api;

import static org.leanlang.radar.codegen.jooq.Tables.METRICS;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.auth.Auth;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.leanlang.radar.server.api.auth.Admin;
import org.leanlang.radar.server.repos.Repos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/admin/repos/{repo}/metrics/delete/")
public record ResAdminRepoMetricsDelete(Repos repos) {
    private static final Logger log = LoggerFactory.getLogger(ResAdminRepoMetricsDelete.class);

    public record JsonPost(@JsonProperty(required = true) List<String> metrics) {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void post(@Auth Admin admin, @PathParam("repo") String repoName, JsonPost input) {
        repos.repo(repoName).db().writeTransaction(ctx -> {
            for (String metric : input.metrics) {
                log.info("Deleting metric '{}' in repo '{}'", metric, repoName);
                ctx.dsl().deleteFrom(METRICS).where(METRICS.METRIC.eq(metric)).execute();
            }
        });
    }
}
