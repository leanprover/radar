package org.leanlang.radar.server.queue;

import java.util.Optional;

public record Run(String name, String script, String runner, Optional<RunFinished> finished) {}
