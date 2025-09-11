package org.leanlang.radar.server.api;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.Optional;
import org.leanlang.radar.server.runners.Runners;

@Path("/runners/{runner}")
public record ResRunnersRunner(Runners runners) {

    public record JsonPostInput(String token) {}

    public record JsonPost(Instant seen, Optional<Instant> lastSeen) {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public JsonPost post(@PathParam("runner") String name, JsonPostInput input) {
        var runner = runners.runner(name, input.token);
        var lastSeen = runner.lastSeen();
        var seen = runner.see();
        return new JsonPost(seen, lastSeen);
    }
}
