package org.leanlang.radar.server.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.leanlang.radar.server.runners.Runners;

@Path("/queue")
public record ResQueue(Runners runners) {

    public record JsonRunner(String name, Optional<Instant> lastSeen) {}

    public record JsonGet(List<JsonRunner> runners) {}

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonGet get() {
        return new JsonGet(this.runners.getRunners().stream()
                .map(it -> new JsonRunner(it.getConfig().name(), it.lastSeen()))
                .toList());
    }
}
