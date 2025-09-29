package org.leanlang.radar.server.api;

import static org.leanlang.radar.codegen.jooq.Tables.COMMITS;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.leanlang.radar.Constants;
import org.leanlang.radar.server.data.Repo;
import org.leanlang.radar.server.data.Repos;
import org.leanlang.radar.server.queue.JsonRun;
import org.leanlang.radar.server.queue.Queue;
import org.leanlang.radar.server.runners.RunnerStatus;
import org.leanlang.radar.server.runners.Runners;

@Path("/queue/")
public record ResQueue(Repos repos, Runners runners, Queue queue) {
    public record JsonRunner(
            @JsonProperty(required = true) String name,
            @JsonProperty(required = true) boolean connected,
            Optional<Instant> lastSeen) {}

    public record JsonTask(
            @JsonProperty(required = true) String repo,
            @JsonProperty(required = true) String chash,
            @JsonProperty(required = true) String title,
            @JsonProperty(required = true) List<JsonRun> runs) {}

    public record JsonGet(
            @JsonProperty(required = true) List<JsonRunner> runners,
            @JsonProperty(required = true) List<JsonTask> tasks) {}

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonGet get() throws IOException {
        Instant connectedCutoff = Instant.now().minus(Constants.RUNNER_CONNECTED_TIME);

        List<JsonRunner> runners = this.runners.runners().stream()
                .map(runner -> new JsonRunner(
                        runner.name(),
                        runner.status()
                                .map(it -> it.from().isAfter(connectedCutoff))
                                .orElse(false),
                        runner.status().map(RunnerStatus::from)))
                .toList();

        List<JsonTask> tasks = queue.getTasks().stream()
                .map(task -> new JsonTask(
                        task.repo().name(), task.chash(), getCommitTitle(task.repo(), task.chash()), task.runs()))
                .toList();

        return new JsonGet(runners, tasks);
    }

    private static String getCommitTitle(Repo repo, String chash) {
        return repo.db()
                .read()
                .dsl()
                .selectFrom(COMMITS)
                .where(COMMITS.CHASH.eq(chash))
                .fetchOne(COMMITS.MESSAGE_TITLE);
    }
}
