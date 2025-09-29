package org.leanlang.radar.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import org.leanlang.radar.runner.statusupdater.JsonActiveRun;
import org.leanlang.radar.server.queue.Queue;
import org.leanlang.radar.server.runners.Runner;
import org.leanlang.radar.server.runners.RunnerStatus;
import org.leanlang.radar.server.runners.Runners;

@Path(ResQueueRunnerStatus.PATH)
public record ResQueueRunnerStatus(Runners runners, Queue queue) {
    public static final String PATH = "/queue/runner/status/";

    public record JsonPostInput(
            @JsonProperty(required = true) String runner,
            @JsonProperty(required = true) String token,
            Optional<JsonActiveRun> activeRun) {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void post(JsonPostInput input) throws IOException {
        Runner runner = runners.runner(input.runner, input.token);
        RunnerStatus status = new RunnerStatus(Instant.now(), input.activeRun);
        runner.setStatus(status);
    }
}
