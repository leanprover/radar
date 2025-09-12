package org.leanlang.radar.server.api;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.Optional;
import org.leanlang.radar.server.queue.RunId;
import org.leanlang.radar.server.runners.Runners;

@Path(ResQueueRunnerStatus.PATH)
public record ResQueueRunnerStatus(Runners runners) {
    public static final String PATH = "/queue/runner/status";

    public record JsonRunId(String repo, String chash, String script) {}

    public record JsonPostInput(String runner, String token, Optional<JsonRunId> activeRun) {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void post(JsonPostInput input) {
        var runner = runners.runner(input.runner, input.token);
        runner.updateStatus(input.activeRun
                .map(it -> new RunId(it.repo, it.chash, input.runner, it.script))
                .orElse(null));
    }
}
