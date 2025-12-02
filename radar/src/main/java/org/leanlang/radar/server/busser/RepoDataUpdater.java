package org.leanlang.radar.server.busser;

import static org.leanlang.radar.codegen.jooq.Tables.COMMITS;
import static org.leanlang.radar.codegen.jooq.Tables.HISTORY;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.jooq.Configuration;
import org.leanlang.radar.codegen.jooq.tables.records.CommitRelationshipsRecord;
import org.leanlang.radar.codegen.jooq.tables.records.CommitsRecord;
import org.leanlang.radar.codegen.jooq.tables.records.HistoryRecord;
import org.leanlang.radar.server.repos.Repo;
import org.leanlang.radar.server.repos.RepoGit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record RepoDataUpdater(Repo repo) {
    private static final Logger log = LoggerFactory.getLogger(RepoDataUpdater.class);

    public void update() throws GitAPIException {
        log.info("Updating commits for repo {}", repo.name());
        repo.git().fetch();
        repo.gitBench().fetch();
        updateRepoData();
        log.info("Updated commits for repo {}", repo.name());
    }

    private void updateRepoData() {
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
        List<ObjectId> refs = refsInAlphabeticalOrder();

        List<String> hashesInChronologicalOrder;
        if (repo.refParentsNone())
            hashesInChronologicalOrder = refs.stream().map(AnyObjectId::name).toList();
        else if (repo.refParentsFirst()) hashesInChronologicalOrder = chronologicalHashesFirstParent(refs);
        else hashesInChronologicalOrder = chronologicalHashesTopo(refs);

        List<HistoryRecord> records = new ArrayList<>();
        for (int i = 0; i < hashesInChronologicalOrder.size(); i++) {
            records.add(new HistoryRecord(i, hashesInChronologicalOrder.get(i)));
        }

        tx.dsl().deleteFrom(HISTORY).execute();
        tx.dsl().batchInsert(records).execute();
        log.info("Updated {} history commits", records.size());
    }

    private List<ObjectId> refsInAlphabeticalOrder() throws IOException, GitAPIException {
        RepoGit git = repo.git();
        String ref = repo.ref();

        if (!repo.refRegex()) {
            return List.of(git.resolveRef(ref));
        }

        return git.plumbing().getRefDatabase().getRefs().stream()
                .filter(it -> it.getName().matches(ref))
                .sorted(Comparator.comparing(Ref::getName))
                .map(git::resolveRef)
                .distinct()
                .toList();
    }

    // Always following only the first parent, sorted so the oldest commits come first.
    private List<String> chronologicalHashesFirstParent(List<ObjectId> refs) throws IOException {
        if (refs.size() != 1)
            throw new IllegalArgumentException("First-parent chronological history is only supported for a single ref");
        ObjectId startId = refs.getFirst();

        List<String> reverseChronologicalHashes = new ArrayList<>();
        try (RevWalk walk = new RevWalk(repo.git().plumbing())) {
            RevCommit head = walk.parseCommit(startId);
            while (true) {
                reverseChronologicalHashes.add(head.getName());
                RevCommit[] parents = head.getParents();
                if (parents == null || parents.length == 0) break;
                head = walk.parseCommit(parents[0]);
            }
        }

        return reverseChronologicalHashes.reversed();
    }

    // Topologically sorted with the oldest commits coming first.
    private List<String> chronologicalHashesTopo(List<ObjectId> refs) throws IOException, GitAPIException {
        LogCommand logCommand = repo.git().porcelain().log();
        for (ObjectId ref : refs) logCommand.add(ref);
        return StreamSupport.stream(logCommand.call().spliterator(), false)
                .map(AnyObjectId::name)
                .toList()
                .reversed();
    }
}
