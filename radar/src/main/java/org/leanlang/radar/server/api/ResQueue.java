package org.leanlang.radar.server.api;

import static org.leanlang.radar.codegen.jooq.Tables.COMMITS;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.leanlang.radar.server.queue.Queue;
import org.leanlang.radar.server.queue.Run;
import org.leanlang.radar.server.runners.RunnerStatus;
import org.leanlang.radar.server.runners.Runners;

@Path("/queue")
public record ResQueue(Runners runners, Queue queue) {

    public record JsonRunner(String name, Optional<Instant> lastSeen) {}

    public record JsonRun(String runner, String script, String state) {}

    public record JsonTask(String repo, String chash, String title, List<JsonRun> runs) {}

    public record JsonGet(List<JsonRunner> runners, List<JsonTask> tasks) {}

    private static String runState(Run run) {
        if (run.result().isPresent()) {
            if (run.result().get().exitCode() == 0) return "success";
            return "error";
        }
        if (run.active()) return "running";
        return "ready";
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonGet get() throws IOException {

        List<JsonRunner> runners = this.runners.runners().stream()
                .map(runner -> new JsonRunner(runner.name(), runner.status().map(RunnerStatus::from)))
                .toList();

        List<JsonTask> tasks = queue.getAllTasks().stream()
                .map(task -> new JsonTask(
                        task.repo().name(),
                        task.chash(),
                        task.repo()
                                .db()
                                .read()
                                .dsl()
                                .selectFrom(COMMITS)
                                .where(COMMITS.CHASH.eq(task.chash()))
                                .fetchOne(COMMITS.MESSAGE_TITLE),
                        task.runs().stream()
                                .map(run -> new JsonRun(run.runner().name(), run.script(), runState(run)))
                                .toList()))
                .toList();

        return new JsonGet(runners, tasks);
    }
}
