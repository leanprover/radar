package org.leanlang.radar.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.net.URI;
import java.util.List;
import org.leanlang.radar.server.repos.Repo;
import org.leanlang.radar.server.repos.Repos;

@Path("/repos/")
public record ResRepos(Repos repos) {

    public record JsonRepo(
            @JsonProperty(required = true) String name,
            @JsonProperty(required = true) URI url,
            @JsonProperty(required = true) URI benchUrl,
            @JsonProperty(required = true) String description) {
        public JsonRepo(Repo repo) {
            this(
                    repo.config().name(),
                    repo.config().url(),
                    repo.config().benchUrl(),
                    repo.config().description());
        }
    }

    public record JsonGet(@JsonProperty(required = true) List<JsonRepo> repos) {}

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonGet get() {
        List<JsonRepo> repos = this.repos.repos().stream().map(JsonRepo::new).toList();
        return new JsonGet(repos);
    }
}
