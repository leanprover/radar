package org.leanlang.radar.server.api;

import static org.leanlang.radar.codegen.jooq.Tables.COMMITS;
import static org.leanlang.radar.codegen.jooq.Tables.HISTORY;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Optional;
import org.leanlang.radar.server.data.Repo;
import org.leanlang.radar.server.data.Repos;

@Path("/repos/{repo}/history/")
public record ResRepoHistory(Repos repos) {
    public record JsonGet(@JsonProperty(required = true) List<JsonCommit> commits) {}

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonGet get(@PathParam("repo") String name, @QueryParam("n") Optional<Integer> nOptional) {
        Repo repo = repos.repo(name);
        int n = Math.clamp(nOptional.orElse(32), 0, 1000);

        List<JsonCommit> commits = repo
                .db()
                .read()
                .dsl()
                .selectFrom(HISTORY.join(COMMITS).onKey())
                .orderBy(HISTORY.POSITION.desc())
                .limit(n)
                .stream()
                .map(it -> new JsonCommit(it.into(COMMITS)))
                .toList();

        return new JsonGet(commits);
    }
}
