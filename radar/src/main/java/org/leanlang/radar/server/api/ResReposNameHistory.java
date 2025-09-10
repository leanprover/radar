package org.leanlang.radar.server.api;

import static org.leanlang.radar.codegen.jooq.tables.History.HISTORY;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Optional;
import org.leanlang.radar.codegen.jooq.tables.records.HistoryRecord;
import org.leanlang.radar.server.data.Repo;
import org.leanlang.radar.server.data.Repos;

@Path("/repos/{name}/history")
public record ResReposNameHistory(Repos repos) {

    public record JsonGet(List<String> commits, Optional<Integer> nextAt) {}

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonGet get(
            @PathParam("name") String name,
            @QueryParam("n") Optional<Integer> nOptional,
            @QueryParam("at") Optional<Integer> atOptional) {

        Repo repo = repos.repo(name);
        int n = Math.clamp(nOptional.orElse(20), 0, 1000);
        int at = atOptional.orElse(Integer.MAX_VALUE);

        List<HistoryRecord> history = repo
                .db()
                .read()
                .dsl()
                .selectFrom(HISTORY)
                .where(HISTORY.POSITION.lt(at))
                .orderBy(HISTORY.POSITION.desc())
                .limit(n)
                .stream()
                .toList();

        List<String> commits = history.stream().map(HistoryRecord::getChash).toList();

        Optional<Integer> nextAt = Optional.of(history)
                .filter(it -> !it.isEmpty())
                .map(it -> it.getLast().getPosition() - 1)
                .filter(it -> it > 0);

        return new JsonGet(commits, nextAt);
    }
}
