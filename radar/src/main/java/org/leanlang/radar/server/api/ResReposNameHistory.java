package org.leanlang.radar.server.api;

import static org.leanlang.radar.codegen.jooq.Tables.COMMITS;
import static org.leanlang.radar.codegen.jooq.Tables.HISTORY;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.leanlang.radar.server.data.Repo;
import org.leanlang.radar.server.data.Repos;

@Path("/repos/{name}/history")
public record ResReposNameHistory(Repos repos) {

    public record JsonCommit(String chash, String title, String author, String committer, Instant committerTime) {}

    public record JsonGet(List<JsonCommit> commits, Optional<Integer> nextAt) {}

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonGet get(
            @PathParam("name") String name,
            @QueryParam("n") Optional<Integer> nOptional,
            @QueryParam("at") Optional<Integer> atOptional) {

        Repo repo = repos.repo(name);
        int n = Math.clamp(nOptional.orElse(20), 0, 1000);
        int at = atOptional.orElse(Integer.MAX_VALUE);

        var history = repo
                .db()
                .read()
                .dsl()
                .select(
                        HISTORY.POSITION,
                        COMMITS.CHASH,
                        COMMITS.MESSAGE_TITLE,
                        COMMITS.AUTHOR_NAME,
                        COMMITS.COMMITTER_NAME,
                        COMMITS.COMMITTER_TIME)
                .from(HISTORY.join(COMMITS).onKey())
                .where(HISTORY.POSITION.lt(at))
                .orderBy(HISTORY.POSITION.desc())
                .limit(n)
                .stream()
                .toList();

        List<JsonCommit> commits = history.stream()
                .map(it -> new JsonCommit(
                        it.component2(), it.component3(), it.component4(), it.component5(), it.component6()))
                .toList();

        Optional<Integer> nextAt = Optional.of(history)
                .filter(it -> !it.isEmpty())
                .map(it -> it.getLast().component1() - 1)
                .filter(it -> it > 0);

        return new JsonGet(commits, nextAt);
    }
}
