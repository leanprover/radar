package org.leanlang.radar.server.queue;

import java.time.Instant;
import java.util.List;

public record Task(String repo, String chash, List<Run> runs, Instant queued, Instant bumped) {
    public TaskId id() {
        return new TaskId(repo, chash);
    }
}
