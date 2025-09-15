package org.leanlang.radar.server.runners;

import java.time.Instant;
import java.util.Optional;

public record RunnerStatus(Instant from, Optional<RunnerStatusRun> activeRun) {}
