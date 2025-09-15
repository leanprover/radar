package org.leanlang.radar.server.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.net.URI;
import java.util.List;
import org.leanlang.radar.server.data.Repo;
import org.leanlang.radar.server.data.Repos;

@Path("/repos")
public record ResRepos(Repos repos) {

    public record JsonRepo(String name, URI url, String description) {
        public JsonRepo(Repo repo) {
            this(repo.config().name(), repo.config().url(), repo.config().description());
        }
    }

    public record JsonGet(List<JsonRepo> repos) {}

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonGet get() {
        List<JsonRepo> repos = this.repos.repos().stream().map(JsonRepo::new).toList();
        return new JsonGet(repos);
    }
}
