package org.leanlang.radar.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import org.leanlang.radar.runner.supervisor.JsonRunResult;
import org.leanlang.radar.server.busser.Busser;
import org.leanlang.radar.server.queue.Queue;
import org.leanlang.radar.server.runners.Runner;
import org.leanlang.radar.server.runners.Runners;

@Path(ResQueueRunnerFinish.PATH)
public record ResQueueRunnerFinish(Runners runners, Queue queue, Busser busser) {
    public static final String PATH = "/queue/runner/finish/";

    public record JsonPostInput(
            @JsonProperty(required = true) String runner,
            @JsonProperty(required = true) String token,
            @JsonProperty(required = true) JsonRunResult result) {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void post(JsonPostInput input) throws IOException {
        Runner runner = runners.runner(input.runner, input.token);
        queue.finishJob(input.result.repo(), runner.name(), input.result);
        busser.updateGhRepliesForRepo(input.result.repo());
    }
}
