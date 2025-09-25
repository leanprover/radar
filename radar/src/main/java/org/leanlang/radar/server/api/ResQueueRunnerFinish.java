package org.leanlang.radar.server.api;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import org.leanlang.radar.runner.supervisor.JsonRunResult;
import org.leanlang.radar.server.queue.Queue;
import org.leanlang.radar.server.runners.Runner;
import org.leanlang.radar.server.runners.Runners;

@Path(ResQueueRunnerFinish.PATH)
public record ResQueueRunnerFinish(Runners runners, Queue queue) {
    public static final String PATH = "/queue/runner/finish/";

    public record JsonPostInput(@NotNull String runner, @NotNull String token, @NotNull JsonRunResult result) {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void post(JsonPostInput input) throws IOException {
        Runner runner = runners.runner(input.runner, input.token);
        queue.finishJob(input.result.repo(), input.result.toRunResult(runner.name()));
    }
}
