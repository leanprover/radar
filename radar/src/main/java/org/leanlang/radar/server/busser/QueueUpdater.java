package org.leanlang.radar.server.busser;

import static org.leanlang.radar.codegen.jooq.Tables.COMMITS;
import static org.leanlang.radar.codegen.jooq.Tables.HISTORY;
import static org.leanlang.radar.codegen.jooq.Tables.QUEUE;
import static org.leanlang.radar.codegen.jooq.Tables.QUEUE_SEEN;

import java.util.List;
import org.jooq.impl.DSL;
import org.leanlang.radar.Constants;
import org.leanlang.radar.server.queue.Queue;
import org.leanlang.radar.server.repos.Repo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record QueueUpdater(Repo repo, Queue queue) {
    private static final Logger log = LoggerFactory.getLogger(QueueUpdater.class);

    public void update() {
        markAllCommitsSeenOnInitialRun();
        enqueueUnseenHistoricCommits();
    }

    private void markAllCommitsSeenOnInitialRun() {
        // We don't want to add thousands of commits to the queue when we first clone a repo.
        // We detect this case because the list of seen commits is empty.
        repo.db().writeTransaction(ctx -> {
            boolean atLeastOneSeen =
                    ctx.dsl().selectOne().from(QUEUE_SEEN).limit(1).fetch().isNotEmpty();
            if (atLeastOneSeen) return;

            int updated = ctx.dsl()
                    .insertInto(QUEUE_SEEN, QUEUE_SEEN.CHASH)
                    .select(DSL.select(COMMITS.CHASH).from(COMMITS))
                    .execute();

            if (updated > 0) {
                log.info("Marked {} commits as seen initially", updated);
            }
        });
    }

    private void enqueueUnseenHistoricCommits() {
        // Find all commits that are now in the history and have never been in the queue (i.e. seen).
        List<String> toEnqueue = repo.db()
                .read()
                .dsl()
                .selectFrom(HISTORY)
                .whereNotExists(DSL.selectOne().from(QUEUE_SEEN).where(QUEUE_SEEN.CHASH.eq(HISTORY.CHASH)))
                .andNotExists(DSL.selectOne().from(QUEUE).where(QUEUE.CHASH.eq(HISTORY.CHASH)))
                .orderBy(HISTORY.POSITION.asc())
                .fetch(HISTORY.CHASH);

        log.info("Adding {} commits to queue", toEnqueue.size());

        for (String chash : toEnqueue) {
            queue.enqueueSoft(repo.name(), chash, Constants.PRIORITY_NEW_COMMIT);
        }

        log.info("Added {} commits to queue", toEnqueue.size());
    }
}
