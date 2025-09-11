package org.leanlang.radar.server.api;

import static org.leanlang.radar.codegen.jooq.Tables.COMMITS;
import static org.leanlang.radar.codegen.jooq.Tables.COMMIT_RELATIONSHIPS;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.List;
import org.leanlang.radar.codegen.jooq.tables.records.CommitsRecord;
import org.leanlang.radar.server.data.Repo;
import org.leanlang.radar.server.data.Repos;

@Path("/repos/{name}/commits/{chash}")
public record ResReposNameCommitsChash(Repos repos) {

    public record JsonPersonIdent(String name, String email, Instant time, int offset) {}

    public record JsonGet(
            String chash,
            JsonPersonIdent author,
            JsonPersonIdent committer,
            String title,
            String body,
            List<String> parents,
            List<String> children) {}

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonGet get(@PathParam("name") String name, @PathParam("chash") String chash) {
        Repo repo = repos.repo(name);

        CommitsRecord commit = repo.db()
                .read()
                .dsl()
                .selectFrom(COMMITS)
                .where(COMMITS.CHASH.eq(chash))
                .fetchOne();
        if (commit == null) throw new IllegalArgumentException("commit not found");

        List<String> parents = repo.db()
                .read()
                .dsl()
                .selectFrom(COMMIT_RELATIONSHIPS)
                .where(COMMIT_RELATIONSHIPS.CHILD.eq(chash))
                .fetch(COMMIT_RELATIONSHIPS.PARENT);

        List<String> children = repo.db()
                .read()
                .dsl()
                .selectFrom(COMMIT_RELATIONSHIPS)
                .where(COMMIT_RELATIONSHIPS.PARENT.eq(chash))
                .fetch(COMMIT_RELATIONSHIPS.CHILD);

        JsonPersonIdent author = new JsonPersonIdent(
                commit.getAuthorName(), commit.getAuthorEmail(), commit.getAuthorTime(), commit.getAuthorOffset());

        JsonPersonIdent committer = new JsonPersonIdent(
                commit.getCommitterName(),
                commit.getCommitterEmail(),
                commit.getCommitterTime(),
                commit.getCommitterOffset());

        return new JsonGet(
                chash, author, committer, commit.getMessageTitle(), commit.getMessageBody(), parents, children);
    }
}
