package org.leanlang.radar.server.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.leanlang.radar.server.runners.Runner;
import org.leanlang.radar.server.runners.Runners;

@Path("/debug")
public record ResDebug(Runners runners) {

    public record JsonGet(List<String> runners) {}

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonGet get() {
        return new JsonGet(runners.runners().stream().map(Runner::name).sorted().toList());
    }
}
