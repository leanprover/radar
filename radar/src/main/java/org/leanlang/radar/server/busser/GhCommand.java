package org.leanlang.radar.server.busser;

import java.util.Optional;
import org.leanlang.radar.server.repos.RepoGh;
import org.leanlang.radar.server.repos.github.JsonGhComment;

public record GhCommand(
        String ownerAndRepo,
        String id,
        String prNumber,
        String userLogin,
        String replyContent,
        Optional<Resolved> resolved) {

    public record Resolved(String headChash, String baseChash) {}

    public GhCommand(RepoGh repoGh, JsonGhComment comment, String replyContent, Optional<Resolved> resolved) {
        this(
                repoGh.ownerAndRepo(),
                comment.idStr(),
                comment.issueNumberStr(),
                comment.user().login(),
                replyContent,
                resolved);
    }
}
