package org.leanlang.radar.server.busser;

import java.util.Optional;
import org.leanlang.radar.server.repos.RepoGh;
import org.leanlang.radar.server.repos.github.JsonGhComment;
import org.leanlang.radar.server.repos.github.JsonGhPull;

public record GhCommand(
        String owner, String repo, JsonGhComment json, String replyContent, Optional<Resolved> resolved) {

    public record Resolved(JsonGhPull json, String chash, String againstChash) {}

    public GhCommand(RepoGh repoGh, JsonGhComment comment, String replyContent, Optional<Resolved> resolved) {
        this(repoGh.owner(), repoGh.repo(), comment, replyContent, resolved);
    }

    public static boolean isCommand(String body) {
        String text = body.strip();
        return text.equals("!bench") || text.equals("!radar");
    }
}
