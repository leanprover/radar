package org.leanlang.radar.server.api;

import static org.leanlang.radar.codegen.jooq.Tables.COMMITS;
import static org.leanlang.radar.codegen.jooq.Tables.HISTORY;
import static org.leanlang.radar.codegen.jooq.Tables.SIGNIFICANCE_FEED;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.server.repos.Repo;
import org.leanlang.radar.server.repos.Repos;

@Path("/repos/{repo}/history/")
public record ResRepoHistory(Repos repos) {
    public record JsonEntry(@JsonProperty(required = true) JsonCommit commit, @Nullable Boolean significant) {}

    public record JsonGet(@JsonProperty(required = true) List<JsonEntry> entries) {}

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonGet get(@PathParam("repo") String name, @QueryParam("n") Optional<Integer> nOptional) {
        Repo repo = repos.repo(name);
        int n = Math.clamp(nOptional.orElse(32), 0, 1000);

        List<JsonEntry> entries = repo
                .db()
                .read()
                .dsl()
                .selectFrom(HISTORY.join(COMMITS)
                        .onKey()
                        .leftJoin(SIGNIFICANCE_FEED)
                        .onKey())
                .orderBy(HISTORY.POSITION.desc())
                .limit(n)
                .stream()
                .map(row -> {
                    JsonCommit commit = new JsonCommit(row.into(COMMITS));
                    Boolean significance = Optional.ofNullable(
                                    row.into(SIGNIFICANCE_FEED).getSignificant())
                            .map(it -> it != 0)
                            .orElse(null);
                    return new JsonEntry(commit, significance);
                })
                .toList();

        return new JsonGet(entries);
    }
}
