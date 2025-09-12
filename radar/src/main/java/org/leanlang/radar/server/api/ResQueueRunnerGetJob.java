package org.leanlang.radar.server.api;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.Optional;
import org.leanlang.radar.server.runners.Runners;

@Path(ResQueueRunnerGetJob.PATH)
public record ResQueueRunnerGetJob(Runners runners) {
    public static final String PATH = "/queue/runner/getJob";

    public record JsonPostInput(String runner, String token) {}

    public record JsonJob(String repo, String url, String chash, String benchUrl, String benchChash, String script) {}

    public record JsonPost(Instant seen, Optional<Instant> lastSeen) {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void post(JsonPostInput input) {}
}
