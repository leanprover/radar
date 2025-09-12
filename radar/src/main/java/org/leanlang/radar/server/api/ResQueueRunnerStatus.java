package org.leanlang.radar.server.api;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.Optional;
import org.leanlang.radar.server.runners.Runners;

@Path(ResQueueRunnerStatus.PATH)
public record ResQueueRunnerStatus(Runners runners) {
    public static final String PATH = "/queue/runner/status";

    public record JsonPostInput(String runner, String token) {}

    public record JsonPost(Instant seen, Optional<Instant> lastSeen) {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public JsonPost post(JsonPostInput input) {
        var runner = runners.runner(input.runner, input.token);
        var lastSeen = runner.lastSeen();
        var seen = runner.see();
        return new JsonPost(seen, lastSeen);
    }
}
