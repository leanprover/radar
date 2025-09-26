package org.leanlang.radar.server.queue;

import java.time.Instant;

public record RunFinished(Instant startTime, Instant endTime, int exitCode) {}
