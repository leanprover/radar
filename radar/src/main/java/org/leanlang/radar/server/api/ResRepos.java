package org.leanlang.radar.server.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.leanlang.radar.server.config.RadarConfigRepo;

@Path("/repos")
@Produces(MediaType.APPLICATION_JSON)
public record ResRepos(List<RadarConfigRepo> repos) {

    @GET
    public List<JsonRepo> debug() {
        return repos.stream().map(JsonRepo::fromConfig).toList();
    }
}
