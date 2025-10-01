package org.leanlang.radar.server.busser;

import static org.leanlang.radar.codegen.jooq.Tables.COMMITS;
import static org.leanlang.radar.codegen.jooq.Tables.HISTORY;
import static org.leanlang.radar.codegen.jooq.Tables.QUEUE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.jooq.Configuration;
import org.jooq.impl.DSL;
import org.leanlang.radar.Constants;
import org.leanlang.radar.codegen.jooq.tables.records.CommitRelationshipsRecord;
import org.leanlang.radar.codegen.jooq.tables.records.CommitsRecord;
import org.leanlang.radar.codegen.jooq.tables.records.HistoryRecord;
import org.leanlang.radar.server.queue.Queue;
import org.leanlang.radar.server.repos.Repo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record DbUpdater(Repo repo, Queue queue) {
    private static final Logger log = LoggerFactory.getLogger(DbUpdater.class);

    public void updateRepoData() {
        repo.db().writeTransaction(tx -> {
            insertNewCommits(tx);
            updateHistory(tx);
        });
    }

    private void insertNewCommits(Configuration tx) throws IOException, GitAPIException {
        Set<String> existing = tx.dsl().selectFrom(COMMITS).fetchSet(COMMITS.CHASH);

        List<CommitsRecord> commitsToInsert = new ArrayList<>();
        List<CommitRelationshipsRecord> relationshipsToInsert = new ArrayList<>();

        for (RevCommit commit : repo.git().porcelain().log().all().call()) {
            if (!existing.contains(commit.name())) {
                CommitsRecord commitRecord = new CommitsRecord();
                commitRecord.setChash(commit.name());

                PersonIdent author = commit.getAuthorIdent();
                commitRecord.setAuthorName(author.getName());
                commitRecord.setAuthorEmail(author.getEmailAddress());
                commitRecord.setAuthorTime(author.getWhenAsInstant());
                commitRecord.setAuthorOffset(author.getZoneOffset().getTotalSeconds());

                PersonIdent committer = commit.getCommitterIdent();
                commitRecord.setCommitterName(committer.getName());
                commitRecord.setCommitterEmail(committer.getEmailAddress());
                commitRecord.setCommitterTime(committer.getWhenAsInstant());
                commitRecord.setCommitterOffset(committer.getZoneOffset().getTotalSeconds());

                String[] parts = commit.getFullMessage().split("\n\n", 2);
                assert parts.length >= 1 && parts.length <= 2;
                String title = parts[0].strip();
                String body = null;
                if (parts.length > 1) body = parts[1].strip();
                commitRecord.setMessageTitle(title);
                commitRecord.setMessageBody(body);

                if (existing.isEmpty()) {
                    // We don't want to add thousands of commits to the queue when we first clone a repo.
                    commitRecord.setSeen(1);
                }

                commitsToInsert.add(commitRecord);

                RevCommit[] parents = commit.getParents();
                for (int i = 0; i < parents.length; i++) {
                    relationshipsToInsert.add(new CommitRelationshipsRecord(commit.name(), parents[i].name(), i));
                }
            }
        }

        tx.dsl().batchInsert(commitsToInsert).execute();
        tx.dsl().batchInsert(relationshipsToInsert).execute();
        log.info("Inserted {} commits and {} relationships", commitsToInsert.size(), relationshipsToInsert.size());
    }

    private void updateHistory(Configuration tx) throws IOException, GitAPIException {
        LogCommand logCommand = repo.git().porcelain().log();

        for (String refName : repo.track()) {
            ObjectId id = repo.git().plumbing().resolve(refName);
            if (id == null) continue;
            logCommand.add(id);
        }

        // Topologically sorted with the oldest commits coming first.
        List<String> hashesInChronologicalOrder = StreamSupport.stream(
                        logCommand.call().spliterator(), false)
                .map(AnyObjectId::name)
                .toList()
                .reversed();

        List<HistoryRecord> records = new ArrayList<>();
        for (int i = 0; i < hashesInChronologicalOrder.size(); i++) {
            records.add(new HistoryRecord(i, hashesInChronologicalOrder.get(i)));
        }

        tx.dsl().deleteFrom(HISTORY).execute();
        tx.dsl().batchInsert(records).execute();
        log.info("Updated {} history commits", records.size());
    }

    public void updateQueue() {
        // Find all commits that are now in the history and have never been in the queue (i.e. seen).
        List<String> toEnqueue = repo.db()
                .read()
                .dsl()
                .selectFrom(HISTORY.join(COMMITS).onKey())
                .where(COMMITS.SEEN.eq(0))
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
