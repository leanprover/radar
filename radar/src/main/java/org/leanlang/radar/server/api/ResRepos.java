package org.leanlang.radar.server.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.net.URI;
import java.util.List;
import org.leanlang.radar.server.data.Repos;

@Path("/repos")
public record ResRepos(Repos repos) {

    public record JsonRepo(String name, URI url, String description) {}

    public record JsonGet(List<JsonRepo> repos) {}

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonGet get() {
        List<JsonRepo> repos = this.repos.getRepos().stream()
                .map(it -> new JsonRepo(
                        it.getConfig().name(),
                        it.getConfig().url(),
                        it.getConfig().description()))
                .toList();

        return new JsonGet(repos);
    }
}
