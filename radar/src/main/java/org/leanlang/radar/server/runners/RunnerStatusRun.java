package org.leanlang.radar.server.runners;

import java.time.Instant;
import java.util.List;
import org.leanlang.radar.runner.supervisor.JsonOutputLine;
import org.leanlang.radar.server.queue.Job;

public record RunnerStatusRun(Job job, Instant startTime, List<JsonOutputLine> lastLines) {}
