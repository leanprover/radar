package org.leanlang.radar.server.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.net.URI;
import java.util.List;
import org.leanlang.radar.server.config.ServerConfigRepo;

@Path("/repos")
@Produces(MediaType.APPLICATION_JSON)
public record ResRepos(List<ServerConfigRepo> repos) {

    public record JsonRepo(String name, URI url, String description) {}

    public record JsonGet(List<JsonRepo> repos) {}

    @GET
    public JsonGet debug() {
        List<JsonRepo> repos = this.repos.stream()
                .map(it -> new JsonRepo(it.name(), it.url(), it.description()))
                .toList();

        return new JsonGet(repos);
    }
}
