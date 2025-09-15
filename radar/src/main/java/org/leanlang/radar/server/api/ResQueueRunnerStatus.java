package org.leanlang.radar.server.api;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import org.leanlang.radar.server.queue.Queue;
import org.leanlang.radar.server.queue.TaskId;
import org.leanlang.radar.server.runners.RunnerStatus;
import org.leanlang.radar.server.runners.RunnerStatusRun;
import org.leanlang.radar.server.runners.Runners;

@Path(ResQueueRunnerStatus.PATH)
public record ResQueueRunnerStatus(Runners runners, Queue queue) {
    public static final String PATH = "/queue/runner/status";

    public record JsonRun(String repo, String chash, String script) {}

    public record JsonPostInput(String runner, String token, Optional<JsonRun> activeRun) {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void post(JsonPostInput input) throws IOException {
        var runner = runners.runner(input.runner, input.token);

        RunnerStatus status = new RunnerStatus(
                Instant.now(), input.activeRun.map(it -> new RunnerStatusRun(it.repo, it.chash, it.script)));

        runner.setStatus(status);

        if (status.activeRun().isPresent()) {
            RunnerStatusRun run = status.activeRun().get();
            queue.ensureActiveTaskExists(new TaskId(run.repo(), run.chash()));
        }
    }
}
