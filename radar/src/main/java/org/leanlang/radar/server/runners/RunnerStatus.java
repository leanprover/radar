package org.leanlang.radar.server.runners;

import java.time.Instant;
import java.util.Optional;
import org.leanlang.radar.runner.statusupdater.JsonActiveRun;

public record RunnerStatus(Instant from, Optional<JsonActiveRun> activeRun) {}
