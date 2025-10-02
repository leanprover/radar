package org.leanlang.radar.server.busser;

import java.util.Optional;

public record GhCommand(String repo, String id, String prNumber, String replyContent, Optional<Resolved> resolved) {
    public record Resolved(String headChash, String baseChash) {}
}
