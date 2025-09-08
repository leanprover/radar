package org.leanlang.radar.server.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.leanlang.radar.server.runners.Runners;

@Path("/debug")
@Produces(MediaType.APPLICATION_JSON)
public record ResDebug(Runners runners) {

    public record JsonGet(List<String> runners) {}

    @GET
    public JsonGet debug() {
        return new JsonGet(runners.getRunners().stream()
                .map(it -> it.getConfig().name())
                .sorted()
                .toList());
    }
}
