package org.leanlang.radar.server.api;

import static org.leanlang.radar.codegen.jooq.Tables.COMMIT_RELATIONSHIPS;
import static org.leanlang.radar.codegen.jooq.Tables.HISTORY;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Optional;
import org.leanlang.radar.codegen.jooq.tables.History;
import org.leanlang.radar.server.compare.CommitComparer;
import org.leanlang.radar.server.compare.JsonCommitComparison;
import org.leanlang.radar.server.queue.Queue;
import org.leanlang.radar.server.repos.Repo;
import org.leanlang.radar.server.repos.Repos;

@Path("/compare/{repo}/{first}/{second}/")
public record ResCompare(Queue queue, Repos repos) {

    public record JsonGet(
            Optional<String> chashFirst,
            Optional<String> chashSecond,
            @JsonProperty(required = true) JsonCommitComparison comparison) {}

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonGet get(
            @PathParam("repo") String repoName,
            @PathParam("first") String paramFirst,
            @PathParam("second") String paramSecond) {

        Repo repo = repos.repo(repoName);

        Optional<String> chashFirst = resolveRelativeTo(repo, paramFirst, paramSecond);
        Optional<String> chashSecond = resolveRelativeTo(repo, paramSecond, paramFirst);

        JsonCommitComparison comparison =
                CommitComparer.compareCommits(queue, repo, chashFirst.orElse(null), chashSecond.orElse(null));

        return new JsonGet(chashFirst, chashSecond, comparison);
    }

    private Optional<String> resolveRelativeTo(Repo repo, String chash, String base) {
        return repo.db().readTransactionResult(ctx -> {
            History h1 = HISTORY.as("h1");
            History h2 = HISTORY.as("h2");

            if (chash.equals("parent")) {
                String result = ctx.dsl()
                        .selectFrom(h1.join(h2).on(h1.POSITION.add(1).eq(h2.POSITION)))
                        .where(h2.CHASH.eq(base))
                        .fetchOne(h1.CHASH);

                if (result == null) {
                    result = ctx.dsl()
                            .selectFrom(COMMIT_RELATIONSHIPS)
                            .where(COMMIT_RELATIONSHIPS.CHILD.eq(base))
                            .orderBy(COMMIT_RELATIONSHIPS.PARENT)
                            .limit(1)
                            .fetchOne(COMMIT_RELATIONSHIPS.PARENT);
                }

                return Optional.ofNullable(result);
            }

            if (chash.equals("child")) {
                String result = ctx.dsl()
                        .selectFrom(h1.join(h2).on(h1.POSITION.add(1).eq(h2.POSITION)))
                        .where(h1.CHASH.eq(base))
                        .fetchOne(h2.CHASH);
                return Optional.ofNullable(result);
            }

            return Optional.of(chash);
        });
    }
}
