package org.leanlang.radar.server.api;

import static org.leanlang.radar.codegen.jooq.Tables.RUNS;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.leanlang.radar.codegen.jooq.tables.records.RunsRecord;
import org.leanlang.radar.runner.supervisor.JsonOutputLine;
import org.leanlang.radar.server.queue.Queue;
import org.leanlang.radar.server.repos.Repo;
import org.leanlang.radar.server.repos.Repos;

@Path("/commits/{repo}/{chash}/runs/{run}/")
public record ResCommitRun(Repos repos, Queue queue) {

    public record JsonGet(
            @JsonProperty(required = true) String runner,
            @JsonProperty(required = true) String script,
            @JsonProperty(required = true) String benchChash,
            @JsonProperty(required = true) Instant startTime,
            @JsonProperty(required = true) Instant endTime,
            Optional<Instant> scriptStartTime,
            Optional<Instant> scriptEndTime,
            @JsonProperty(required = true) int exitCode,
            Optional<List<JsonOutputLine>> lines) {}

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

        Optional<List<JsonOutputLine>> lines;
        try {
            lines = Optional.of(repo.loadRunLog(chash, runName));
        } catch (Exception e) {
            lines = Optional.empty();
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
