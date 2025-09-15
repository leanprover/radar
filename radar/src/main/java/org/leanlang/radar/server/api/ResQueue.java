package org.leanlang.radar.server.api;

import static org.leanlang.radar.codegen.jooq.Tables.COMMITS;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.leanlang.radar.server.data.Repo;
import org.leanlang.radar.server.data.Repos;
import org.leanlang.radar.server.queue.ActiveTask;
import org.leanlang.radar.server.queue.Queue;
import org.leanlang.radar.server.queue.Run;
import org.leanlang.radar.server.queue.RunResult;
import org.leanlang.radar.server.queue.Task;
import org.leanlang.radar.server.runners.RunnerStatus;
import org.leanlang.radar.server.runners.Runners;

@Path("/queue")
public record ResQueue(Repos repos, Runners runners, Queue queue) {

    public record JsonActiveRun(String repo, String chash, String script) {}

    public record JsonRunner(
            String name, boolean connected, Optional<Instant> lastSeen, Optional<JsonActiveRun> activeRun) {}

    public record JsonRun(String runner, String script, Optional<Integer> exitCode) {}

    public record JsonTask(String repo, String chash, String title, List<JsonRun> runs) {}

    public record JsonGet(List<JsonRunner> runners, List<JsonTask> tasks) {}

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonGet get() throws IOException {
        Instant connectedCutoff = Instant.now().minus(Duration.ofSeconds(10));

        List<JsonRunner> runners = this.runners.runners().stream()
                .map(runner -> new JsonRunner(
                        runner.name(),
                        runner.status()
                                .map(it -> it.from().isAfter(connectedCutoff))
                                .orElse(false),
                        runner.status().map(RunnerStatus::from),
                        runner.status()
                                .flatMap(RunnerStatus::activeRun)
                                .map(run -> new JsonActiveRun(run.repo(), run.chash(), run.script()))))
                .toList();

        List<JsonTask> tasks = new ArrayList<>();

        for (ActiveTask task : queue.getActiveTasks()) {
            List<RunResult> results = task.results();
            List<JsonRun> runs = getRunsWithExitCode(task.runs(), results);
            String title = getCommitTitle(task.repo(), task.chash());
            tasks.add(new JsonTask(task.repo().name(), task.chash(), title, runs));
        }

        for (Task task : queue.getQueuedTasks()) {
            List<JsonRun> runs = getRunsWithExitCode(task.runs(), List.of());
            String title = getCommitTitle(repos.repo(task.repo()), task.chash());
            tasks.add(new JsonTask(task.repo(), task.chash(), title, runs));
        }

        return new JsonGet(runners, tasks);
    }

    private static List<JsonRun> getRunsWithExitCode(List<Run> runs, List<RunResult> results) {
        // Currently O(n*m), maybe optimize?
        return runs.stream()
                .map(run -> new JsonRun(
                        run.runner(),
                        run.script(),
                        results.stream()
                                .filter(it -> it.runner().equals(run.runner())
                                        && it.script().equals(run.script()))
                                .findFirst()
                                .map(RunResult::exitCode)))
                .toList();
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
