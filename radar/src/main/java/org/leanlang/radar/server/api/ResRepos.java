package org.leanlang.radar.server.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.leanlang.radar.server.RepoConfig;

import java.util.Map;
import java.util.stream.Collectors;

@Path("/repos")
@Produces(MediaType.APPLICATION_JSON)
public record ResRepos(Map<String, RepoConfig> repos) {

    @GET
    public Map<String, JsonRepo> debug() {
        return repos.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> JsonRepo.fromRepoConfig(e.getValue())));
    }
}
