package org.leanlang.radar.server.queue;

import java.time.Instant;
import java.util.List;
import org.leanlang.radar.server.repos.Repo;

public record Task(Repo repo, String chash, Instant queued, Instant bumped, List<Run> runs) {}
