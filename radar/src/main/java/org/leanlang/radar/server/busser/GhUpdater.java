package org.leanlang.radar.server.busser;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.leanlang.radar.codegen.jooq.Tables;
import org.leanlang.radar.codegen.jooq.tables.records.GithubLastCheckedRecord;
import org.leanlang.radar.server.queue.Queue;
import org.leanlang.radar.server.repos.Repo;
import org.leanlang.radar.server.repos.RepoGh;
import org.leanlang.radar.server.repos.github.JsonGhComment;

public record GhUpdater(Repo repo, Queue queue, RepoGh repoGh) {
    public List<JsonGhComment> searchForComments() {
        Instant now = Instant.now();

        Instant since = Optional.ofNullable(repo.db()
                        .read()
                        .dsl()
                        .selectFrom(Tables.GITHUB_LAST_CHECKED)
                        .fetchOne())
                .map(GithubLastCheckedRecord::getLastCheckedTime)
                .orElse(now);

        return repoGh.getComments(since);
    }

    public void addCommands(List<JsonGhComment> comments) {
        // TODO Implement
    }

    public void executeCommands() {
        // TODO Implement
    }

    public void updateReplies() {
        // TODO Implement
    }
}
