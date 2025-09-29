package org.leanlang.radar.server.api;

import static org.leanlang.radar.codegen.jooq.Tables.COMMITS;
import static org.leanlang.radar.codegen.jooq.Tables.COMMIT_RELATIONSHIPS;
import static org.leanlang.radar.codegen.jooq.Tables.HISTORY;
import static org.leanlang.radar.codegen.jooq.Tables.RUNS;

import com.fasterxml.jackson.annotation.JsonProperty;
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
import org.leanlang.radar.server.queue.JsonRun;
import org.leanlang.radar.server.queue.Queue;
import org.leanlang.radar.server.queue.Task;

@Path("/commits/{repo}/{chash}/")
public record ResCommit(Repos repos, Queue queue) {

    public record JsonPersonIdent(
            @JsonProperty(required = true) String name,
            @JsonProperty(required = true) String email,
            @JsonProperty(required = true) Instant time,
            @JsonProperty(required = true) int offset) {}

    public record JsonLinkedCommit(
            @JsonProperty(required = true) String chash,
            @JsonProperty(required = true) String title,
            @JsonProperty(required = true) boolean tracked) {}

    public record JsonGet(
            @JsonProperty(required = true) String chash,
            @JsonProperty(required = true) JsonPersonIdent author,
            @JsonProperty(required = true) JsonPersonIdent committer,
            @JsonProperty(required = true) String title,
            Optional<String> body,
            @JsonProperty(required = true) List<JsonLinkedCommit> parents,
            @JsonProperty(required = true) List<JsonLinkedCommit> children,
            @JsonProperty(required = true) List<JsonRun> runs) {}

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonGet get(@PathParam("repo") String repoName, @PathParam("chash") String chash) {
        Repo repo = repos.repo(repoName);

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

        List<JsonRun> runs = queue.getTask(repoName, chash)
                .map(Task::runs)
                .orElseGet(() -> repo.db().read().dsl().selectFrom(RUNS).where(RUNS.CHASH.eq(chash)).stream()
                        .map(it -> new JsonRun(
                                it.getName(),
                                it.getScript(),
                                it.getRunner(),
                                Optional.empty(),
                                Optional.of(
                                        new JsonRun.Finished(it.getStartTime(), it.getEndTime(), it.getExitCode()))))
                        .toList());

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
