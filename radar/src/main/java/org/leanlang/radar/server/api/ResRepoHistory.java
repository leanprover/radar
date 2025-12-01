package org.leanlang.radar.server.api;

import static org.leanlang.radar.codegen.jooq.Tables.COMMITS;
import static org.leanlang.radar.codegen.jooq.Tables.HISTORY;
import static org.leanlang.radar.codegen.jooq.Tables.QUEUE;
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
import org.jooq.Configuration;
import org.jooq.Record14;
import org.jooq.SelectSelectStep;
import org.jooq.impl.DSL;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.server.repos.Repo;
import org.leanlang.radar.server.repos.Repos;

@Path("/repos/{repo}/history/")
public record ResRepoHistory(Repos repos) {
    public record JsonEntry(
            @JsonProperty(required = true) JsonCommit commit,
            boolean hasRuns,
            boolean enqueued,
            @Nullable Boolean significant) {}

    public record JsonGet(
            @JsonProperty(required = true) int total,
            @JsonProperty(required = true) List<JsonEntry> entries) {
        public JsonGet() {
            this(0, List.of());
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonGet get(
            @PathParam("repo") String name,
            @QueryParam("n") Optional<Integer> nOptional,
            @QueryParam("skip") Optional<Integer> skipOptional,
            @QueryParam("s") Optional<String> queryOptional) {

        Repo repo = repos.repo(name);
        int n = Math.clamp(nOptional.orElse(32), 0, 1000);
        int skip = Math.max(skipOptional.orElse(0), 0);
        Optional<String> query = queryOptional.filter(it -> !it.isEmpty());

        return repo.db().readTransactionResult(ctx -> query.map(s -> searchHistory(ctx, skip, n, s))
                .orElseGet(() -> getChronologicalHistory(ctx, skip, n)));
    }

    private static SelectSelectStep<
                    Record14<
                            String,
                            String,
                            String,
                            Instant,
                            Integer,
                            String,
                            String,
                            Instant,
                            Integer,
                            String,
                            String,
                            Boolean,
                            Boolean,
                            Integer>>
            select(Configuration ctx) {
        return ctx.dsl()
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
                        DSL.exists(DSL.selectOne().from(QUEUE).where(QUEUE.CHASH.eq(COMMITS.CHASH))),
                        SIGNIFICANCE_FEED.SIGNIFICANT);
    }

    private static JsonEntry mkEntry(
            Record14<
                            String,
                            String,
                            String,
                            Instant,
                            Integer,
                            String,
                            String,
                            Instant,
                            Integer,
                            String,
                            String,
                            Boolean,
                            Boolean,
                            Integer>
                    row) {
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
        Boolean enqueued = row.value13();
        Integer feedSignificant = row.value14(); // Nullable

        JsonCommit.Ident author = new JsonCommit.Ident(authorName, authorEmail, authorTime, authorOffset);
        JsonCommit.Ident committer =
                new JsonCommit.Ident(committerName, committerEmail, committerTime, committerOffset);
        JsonCommit commit = new JsonCommit(chash, author, committer, messageTitle, Optional.ofNullable(messageBody));
        Boolean significant =
                Optional.ofNullable(feedSignificant).map(it -> it != 0).orElse(null);
        return new JsonEntry(commit, hasRuns, enqueued, significant);
    }

    private static JsonGet getChronologicalHistory(Configuration ctx, int skip, int n) {
        int total = ctx.dsl().fetchCount(HISTORY);

        List<JsonEntry> entries = select(ctx)
                .from(HISTORY.join(COMMITS).onKey().leftJoin(SIGNIFICANCE_FEED).onKey())
                .orderBy(HISTORY.POSITION.desc())
                .offset(skip)
                .limit(n)
                .stream()
                .map(ResRepoHistory::mkEntry)
                .toList();

        return new JsonGet(total, entries);
    }

    private static JsonGet searchHistory(Configuration ctx, int skip, int n, String query) {
        JsonGet result = searchHistoryByChash(ctx, skip, n, query);
        if (result.total > 0) return result;
        return searchHistoryBySubstring(ctx, skip, n, query);
    }

    private static JsonGet searchHistoryByChash(Configuration ctx, int skip, int n, String query) {
        if (query.length() < 4) return new JsonGet();

        // There probably won't be many entries whose hashes share the first five characters,
        // so always fetching them should be fine.
        List<JsonEntry> entries = select(ctx)
                .from(COMMITS.leftJoin(SIGNIFICANCE_FEED).onKey())
                .where(COMMITS.CHASH.startsWith(query))
                .orderBy(COMMITS.CHASH.asc())
                .stream()
                .map(ResRepoHistory::mkEntry)
                .toList();

        return new JsonGet(entries.size(), entries.stream().skip(skip).limit(n).toList());
    }

    private static JsonGet searchHistoryBySubstring(Configuration ctx, int skip, int n, String query) {
        var sql = select(ctx)
                .from(COMMITS.leftJoin(SIGNIFICANCE_FEED).onKey().join(HISTORY).onKey())
                .where(COMMITS.MESSAGE_TITLE.contains(query))
                .or(COMMITS.MESSAGE_BODY.contains(query))
                .or(COMMITS.AUTHOR_NAME.contains(query))
                .or(COMMITS.AUTHOR_EMAIL.contains(query))
                .or(COMMITS.COMMITTER_NAME.contains(query))
                .or(COMMITS.COMMITTER_EMAIL.contains(query))
                .orderBy(COMMITS.COMMITTER_TIME.desc());

        int total = ctx.dsl().fetchCount(sql);

        List<JsonEntry> entries =
                sql.limit(n).offset(skip).stream().map(ResRepoHistory::mkEntry).toList();

        return new JsonGet(total, entries);
    }
}
