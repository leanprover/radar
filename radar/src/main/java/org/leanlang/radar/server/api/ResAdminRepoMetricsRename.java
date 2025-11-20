package org.leanlang.radar.server.api;

import static org.leanlang.radar.codegen.jooq.Tables.MEASUREMENTS;
import static org.leanlang.radar.codegen.jooq.Tables.METRICS;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.auth.Auth;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import org.jooq.impl.DSL;
import org.leanlang.radar.codegen.jooq.tables.Measurements;
import org.leanlang.radar.codegen.jooq.tables.records.MetricsRecord;
import org.leanlang.radar.server.api.auth.Admin;
import org.leanlang.radar.server.repos.Repos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/admin/repos/{repo}/metrics/rename/")
public record ResAdminRepoMetricsRename(Repos repos) {

    private static final Logger log = LoggerFactory.getLogger(ResAdminRepoMetricsRename.class);

    public record JsonPost(@JsonProperty(required = true) Map<String, String> metrics) {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void get(@Auth Admin admin, @PathParam("repo") String repoName, JsonPost input) {
        repos.repo(repoName).db().writeTransaction(ctx -> {
            for (Map.Entry<String, String> entry : input.metrics.entrySet()) {
                log.info("Renaming metric '{}' to '{}' in repo '{}'", entry.getKey(), entry.getValue(), repoName);

                MetricsRecord metricsRecord = ctx.dsl()
                        .selectFrom(METRICS)
                        .where(METRICS.METRIC.eq(entry.getKey()))
                        .fetchOne();

                // Ensure the new metric exists while keeping the old one around
                // because of the ON DELETE CASCADE constraint in the MEASUREMENTS table.
                if (metricsRecord != null) {
                    ctx.dsl()
                            .insertInto(METRICS, METRICS.METRIC, METRICS.UNIT)
                            .values(entry.getValue(), metricsRecord.getUnit())
                            .onDuplicateKeyIgnore()
                            .execute();
                }

                // Rename measurements for which there is no naming collision
                Measurements m2 = MEASUREMENTS.as("m2");
                ctx.dsl()
                        .update(MEASUREMENTS)
                        .set(MEASUREMENTS.METRIC, entry.getValue())
                        .where(MEASUREMENTS.METRIC.eq(entry.getKey()))
                        .andNotExists(DSL.selectOne()
                                .from(m2)
                                .where(m2.CHASH.eq(MEASUREMENTS.CHASH))
                                .and(m2.METRIC.eq(entry.getValue())))
                        .execute();

                // Delete the old metric
                // Includes any left-over measurements, thanks to ON DELETE CASCADE
                ctx.dsl()
                        .deleteFrom(METRICS)
                        .where(METRICS.METRIC.eq(entry.getKey()))
                        .execute();
            }
        });
    }
}
