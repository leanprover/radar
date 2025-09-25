package org.leanlang.radar.runner.supervisor;

import java.time.Instant;
import org.leanlang.radar.server.queue.Job;

public record SupervisorStatus(Job job, Instant startTime, OutputLines lines) {}
