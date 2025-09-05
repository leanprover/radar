package org.leanlang.radar.server.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/debug")
@Produces(MediaType.APPLICATION_JSON)
public class ResDebug {
    @GET
    public JsonDebug debug() {
        return new JsonDebug("Hello world!");
    }
}
