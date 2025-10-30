package org.leanlang.radar.server.busser;

import static org.leanlang.radar.codegen.jooq.Tables.HISTORY;
import static org.leanlang.radar.codegen.jooq.Tables.RUNS;
import static org.leanlang.radar.codegen.jooq.Tables.SIGNIFICANCE_FEED;

import java.util.List;
import java.util.Optional;
import org.jooq.Record3;
import org.jooq.impl.DSL;
import org.leanlang.radar.codegen.jooq.tables.records.HistoryRecord;
import org.leanlang.radar.server.compare.CommitComparer;
import org.leanlang.radar.server.compare.JsonCommitComparison;
import org.leanlang.radar.server.repos.Repo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record SignificanceUpdater(Repo repo) {
    private static final Logger log = LoggerFactory.getLogger(SignificanceUpdater.class);

    public void update() {
        log.info("Updating significances for repo {}", repo.name());
        addRunlessCommitsToFeedOnInitialRun();
        computeSequentialSignificances();
        log.info("Updated significances for repo {}", repo.name());
    }

    private void addRunlessCommitsToFeedOnInitialRun() {
        // We don't want to check potentially thousands of run-less commits for significance on our first run.
        // Thus, we add all commits up until the first commit with a run to the empty significance feed.
        repo.db().writeTransaction(ctx -> {
            boolean feedEmpty = ctx.dsl()
                    .selectOne()
                    .from(SIGNIFICANCE_FEED)
                    .limit(1)
                    .fetch()
                    .isEmpty();
            if (!feedEmpty) return;

            Optional<HistoryRecord> firstHistoryCommitWithRun = Optional.ofNullable(ctx.dsl()
                    .selectFrom(HISTORY)
                    .whereExists(DSL.selectOne().from(RUNS).where(RUNS.CHASH.eq(HISTORY.CHASH)))
                    .orderBy(HISTORY.POSITION.asc())
                    .limit(1)
                    .fetchOne());

            int firstForbiddenIndex =
                    firstHistoryCommitWithRun.map(HistoryRecord::getPosition).orElse(Integer.MAX_VALUE);

            int added = ctx.dsl()
                    .insertInto(SIGNIFICANCE_FEED, SIGNIFICANCE_FEED.CHASH)
                    .select(DSL.select(HISTORY.CHASH).from(HISTORY).where(HISTORY.POSITION.lt(firstForbiddenIndex)))
                    .execute();
            if (added > 0) {
                log.info(
                        "Added {} run-less commits to significance feed (before index {})", added, firstForbiddenIndex);
            }
        });
    }

    private void computeSequentialSignificances() {
        // Not in a transaction because it may take a while to complete,
        // and we're only modifying the significance feed table.
        // If anything changes while we're doing this,
        // we're going to notice at the next iteration and fill in the relevant significances then.
        // Eventually, we should reach a consistent state.

        List<Record3<String, Boolean, Boolean>> commits = repo
                .db()
                .read()
                .dsl()
                .select(
                        HISTORY.CHASH,
                        DSL.exists(DSL.selectOne().from(RUNS).where(RUNS.CHASH.eq(HISTORY.CHASH))),
                        DSL.exists(DSL.selectOne()
                                .from(SIGNIFICANCE_FEED)
                                .where(SIGNIFICANCE_FEED.CHASH.eq(HISTORY.CHASH))))
                .from(HISTORY)
                .orderBy(HISTORY.POSITION.asc())
                .stream()
                .toList();

        for (int i = 0; i < commits.size(); i++) {
            Record3<String, Boolean, Boolean> cur = commits.get(i);
            String curHash = cur.value1();
            Boolean curHasRuns = cur.value2();
            Boolean curInFeed = cur.value3();

            if (curInFeed) continue; // No need to recompute
            if (!curHasRuns) {
                log.info("Reached runless commit {}, stopping", curHash);
                break; // Feed must be filled in sequentially, so we stop at the first gap
            }

            String prevHash = null;
            if (i > 0) prevHash = commits.get(i - 1).value1();

            JsonCommitComparison comparison = CommitComparer.compareCommits(repo, prevHash, curHash);
            boolean significant = comparison.significant();
            log.info("Adding commit {} to feed as {}", curHash, significant ? "significant" : "insignificant");
            repo.db().writeTransaction(ctx -> ctx.dsl()
                    .insertInto(SIGNIFICANCE_FEED, SIGNIFICANCE_FEED.CHASH, SIGNIFICANCE_FEED.SIGNIFICANT)
                    .values(curHash, significant ? 1 : 0)
                    .onDuplicateKeyIgnore()
                    .execute());
        }
    }
}
