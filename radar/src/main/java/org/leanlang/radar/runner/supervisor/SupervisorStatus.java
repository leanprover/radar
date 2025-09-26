package org.leanlang.radar.runner.supervisor;

import java.time.Instant;

public record SupervisorStatus(JsonJob job, Instant startTime, OutputLines lines) {}
