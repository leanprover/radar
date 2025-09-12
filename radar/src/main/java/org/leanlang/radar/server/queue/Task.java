package org.leanlang.radar.server.queue;

import java.time.Instant;
import java.util.List;
import org.leanlang.radar.server.data.Repo;

public record Task(Repo repo, String chash, String benchChash, List<Run> runs, Instant queued, Instant bumped) {
    public TaskId id() {
        return new TaskId(repo.name(), chash);
    }
}
