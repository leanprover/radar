package org.leanlang.radar.server.runners;

import java.util.List;
import org.leanlang.radar.runner.supervisor.JsonOutputLine;

public record RunnerStatusRun(String repo, String chash, String script, List<JsonOutputLine> lastLines) {}
