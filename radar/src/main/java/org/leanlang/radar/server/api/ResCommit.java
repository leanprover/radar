package org.leanlang.radar.server.api;

import static org.leanlang.radar.codegen.jooq.Tables.COMMITS;
import static org.leanlang.radar.codegen.jooq.Tables.COMMIT_RELATIONSHIPS;
import static org.leanlang.radar.codegen.jooq.Tables.HISTORY;
import static org.leanlang.radar.codegen.jooq.Tables.RUNS;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.leanlang.radar.codegen.jooq.tables.records.CommitsRecord;
import org.leanlang.radar.server.data.Repo;
import org.leanlang.radar.server.data.Repos;

@Path("/commits/{repo}/{chash}/")
public record ResCommit(Repos repos) {

    public record JsonPersonIdent(String name, String email, Instant time, int offset) {}

    public record JsonLinkedCommit(String chash, String title, boolean tracked) {}

    public record JsonRun(
            String name,
            String script,
            String runner,
            String benchChash,
            Instant startTime,
            Instant endTime,
            int exitCode) {}

    public record JsonGet(
            String chash,
            JsonPersonIdent author,
            JsonPersonIdent committer,
            String title,
            Optional<String> body,
            List<JsonLinkedCommit> parents,
            List<JsonLinkedCommit> children,
            List<JsonRun> runs) {}

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonGet get(@PathParam("repo") String name, @PathParam("chash") String chash) {
        Repo repo = repos.repo(name);

        CommitsRecord commit = repo.db()
                .read()
                .dsl()
                .selectFrom(COMMITS)
                .where(COMMITS.CHASH.eq(chash))
                .fetchOne();
        if (commit == null) throw new IllegalArgumentException("commit not found");

        List<JsonLinkedCommit> parents = repo
                .db()
                .read()
                .dsl()
                .select(COMMITS.CHASH, COMMITS.MESSAGE_TITLE, HISTORY.CHASH.isNotNull())
                .from(COMMIT_RELATIONSHIPS
                        .join(COMMITS)
                        .on(COMMITS.CHASH.eq(COMMIT_RELATIONSHIPS.PARENT))
                        .leftJoin(HISTORY)
                        .onKey())
                .where(COMMIT_RELATIONSHIPS.CHILD.eq(chash))
                .orderBy(COMMIT_RELATIONSHIPS.PARENT_POSITION)
                .stream()
                .map(it -> new JsonLinkedCommit(it.component1(), it.component2(), it.component3()))
                .toList();

        List<JsonLinkedCommit> children = repo
                .db()
                .read()
                .dsl()
                .select(COMMITS.CHASH, COMMITS.MESSAGE_TITLE, HISTORY.CHASH.isNotNull())
                .from(COMMIT_RELATIONSHIPS
                        .join(COMMITS)
                        .on(COMMITS.CHASH.eq(COMMIT_RELATIONSHIPS.CHILD))
                        .leftJoin(HISTORY)
                        .onKey())
                .where(COMMIT_RELATIONSHIPS.PARENT.eq(chash))
                .orderBy(HISTORY.CHASH.isNotNull().desc())
                .stream()
                .map(it -> new JsonLinkedCommit(it.component1(), it.component2(), it.component3()))
                .toList();

        JsonPersonIdent author = new JsonPersonIdent(
                commit.getAuthorName(), commit.getAuthorEmail(), commit.getAuthorTime(), commit.getAuthorOffset());

        JsonPersonIdent committer = new JsonPersonIdent(
                commit.getCommitterName(),
                commit.getCommitterEmail(),
                commit.getCommitterTime(),
                commit.getCommitterOffset());

        List<JsonRun> runs = repo.db().read().dsl().selectFrom(RUNS).where(RUNS.CHASH.eq(chash)).stream()
                .map(it -> new JsonRun(
                        it.getName(),
                        it.getScript(),
                        it.getRunner(),
                        it.getChashBench(),
                        it.getStartTime(),
                        it.getEndTime(),
                        it.getExitCode()))
                .toList();

        return new JsonGet(
                chash,
                author,
                committer,
                commit.getMessageTitle(),
                Optional.ofNullable(commit.getMessageBody()),
                parents,
                children,
                runs);
    }
}
