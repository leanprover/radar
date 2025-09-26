package org.leanlang.radar.server.api;

import static org.leanlang.radar.codegen.jooq.Tables.RUNS;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.leanlang.radar.codegen.jooq.tables.records.RunsRecord;
import org.leanlang.radar.runner.supervisor.JsonOutputLine;
import org.leanlang.radar.server.data.Repo;
import org.leanlang.radar.server.data.Repos;
import org.leanlang.radar.server.queue.Queue;
import org.leanlang.radar.server.runners.Runners;

@Path("/commits/{repo}/{chash}/runs/{run}/")
public record ResCommitRun(Repos repos, Runners runners, Queue queue) {

    public record JsonGet(
            String runner,
            String script,
            String benchChash,
            Instant startTime,
            Instant endTime,
            Optional<Instant> scriptStartTime,
            Optional<Instant> scriptEndTime,
            int exitCode,
            List<JsonOutputLine> lines) {}

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonGet get(
            @PathParam("repo") String repoName, @PathParam("chash") String chash, @PathParam("run") String runName)
            throws IOException {
        Repo repo = repos.repo(repoName);

        RunsRecord record = repo.db()
                .read()
                .dsl()
                .selectFrom(RUNS)
                .where(RUNS.CHASH.eq(chash))
                .and(RUNS.NAME.eq(runName))
                .fetchOne();
        if (record == null) throw new NotFoundException();

        List<JsonOutputLine> lines;
        try {
            lines = repo.loadRunLog(chash);
        } catch (Exception e) {
            lines = new ArrayList<>();
        }

        return new JsonGet(
                record.getRunner(),
                record.getScript(),
                record.getChashBench(),
                record.getStartTime(),
                record.getEndTime(),
                Optional.ofNullable(record.getScriptStartTime()),
                Optional.ofNullable(record.getScriptEndTime()),
                record.getExitCode(),
                lines);
    }
}
