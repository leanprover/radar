package org.leanlang.radar.server.api;

import static org.leanlang.radar.codegen.jooq.Tables.RUNS;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.List;
import org.leanlang.radar.server.data.Repo;
import org.leanlang.radar.server.data.Repos;

@Path("/runs/{repo}/{chash}/")
public record ResRuns(Repos repos) {
    public record JsonRun(
            String name,
            String script,
            String runner,
            String benchChash,
            Instant startTime,
            Instant endTime,
            int exitCode) {}

    public record JsonGet(List<JsonRun> runs) {}

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonGet get(@PathParam("repo") String name, @PathParam("chash") String chash) {
        Repo repo = repos.repo(name);

        List<JsonRun> runs = repo.db().read().dsl().selectFrom(RUNS).where(RUNS.CHASH.eq(chash)).stream()
                .map(it -> new JsonRun(
                        it.getName(),
                        it.getScript(),
                        it.getRunner(),
                        it.getChashBench(),
                        it.getStartTime(),
                        it.getEndTime(),
                        it.getExitCode()))
                .toList();

        return new JsonGet(runs);
    }
}
