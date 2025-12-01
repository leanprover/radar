package org.leanlang.radar.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.auth.Auth;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import org.leanlang.radar.server.api.auth.Admin;
import org.leanlang.radar.server.busser.Busser;

@Path("/admin/recompute-significance/")
public record ResAdminRecomputeSignificance(Busser busser) {
    public record JsonPostInput(
            @JsonProperty(required = true) String repo) {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void post(@Auth Admin admin, JsonPostInput input) {
        busser.recomputeSignificance(input.repo);
    }
}
