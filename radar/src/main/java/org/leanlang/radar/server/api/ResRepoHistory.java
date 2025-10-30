package org.leanlang.radar.server.api;

import static org.leanlang.radar.codegen.jooq.Tables.COMMITS;
import static org.leanlang.radar.codegen.jooq.Tables.HISTORY;
import static org.leanlang.radar.codegen.jooq.Tables.RUNS;
import static org.leanlang.radar.codegen.jooq.Tables.SIGNIFICANCE_FEED;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.jooq.impl.DSL;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.server.repos.Repo;
import org.leanlang.radar.server.repos.Repos;

@Path("/repos/{repo}/history/")
public record ResRepoHistory(Repos repos) {
    public record JsonEntry(
            @JsonProperty(required = true) JsonCommit commit, boolean hasRuns, @Nullable Boolean significant) {}

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
                .select(
                        COMMITS.CHASH,
                        COMMITS.AUTHOR_NAME,
                        COMMITS.AUTHOR_EMAIL,
                        COMMITS.AUTHOR_TIME,
                        COMMITS.AUTHOR_OFFSET,
                        COMMITS.COMMITTER_NAME,
                        COMMITS.COMMITTER_EMAIL,
                        COMMITS.COMMITTER_TIME,
                        COMMITS.COMMITTER_OFFSET,
                        COMMITS.MESSAGE_TITLE,
                        COMMITS.MESSAGE_BODY,
                        DSL.exists(DSL.selectOne().from(RUNS).where(RUNS.CHASH.eq(COMMITS.CHASH))),
                        SIGNIFICANCE_FEED.SIGNIFICANT)
                .from(HISTORY.join(COMMITS).onKey().leftJoin(SIGNIFICANCE_FEED).onKey())
                .orderBy(HISTORY.POSITION.desc())
                .limit(n)
                .stream()
                .map(row -> {
                    String chash = row.value1();
                    String authorName = row.value2();
                    String authorEmail = row.value3();
                    Instant authorTime = row.value4();
                    Integer authorOffset = row.value5();
                    String committerName = row.value6();
                    String committerEmail = row.value7();
                    Instant committerTime = row.value8();
                    Integer committerOffset = row.value9();
                    String messageTitle = row.value10();
                    String messageBody = row.value11(); // Nullable
                    Boolean hasRuns = row.value12();
                    Integer feedSignificant = row.value13(); // Nullable

                    JsonCommit.Ident author = new JsonCommit.Ident(authorName, authorEmail, authorTime, authorOffset);
                    JsonCommit.Ident committer =
                            new JsonCommit.Ident(committerName, committerEmail, committerTime, committerOffset);
                    JsonCommit commit =
                            new JsonCommit(chash, author, committer, messageTitle, Optional.ofNullable(messageBody));
                    Boolean significant = Optional.ofNullable(feedSignificant)
                            .map(it -> it != 0)
                            .orElse(null);
                    return new JsonEntry(commit, hasRuns, significant);
                })
                .toList();

        return new JsonGet(entries);
    }
}
