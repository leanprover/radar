package org.leanlang.radar.server.runners;

import java.time.Instant;
import java.util.List;
import org.leanlang.radar.runner.supervisor.JsonJob;
import org.leanlang.radar.runner.supervisor.JsonOutputLine;

public record RunnerStatusRun(JsonJob job, Instant startTime, List<JsonOutputLine> lastLines) {}
