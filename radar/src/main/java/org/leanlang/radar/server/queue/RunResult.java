package org.leanlang.radar.server.queue;

import java.util.List;

public record RunResult(int exitCode, List<RunResultEntry> entries) {}
