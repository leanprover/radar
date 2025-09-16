package org.leanlang.radar.server.queue;

import java.util.Optional;

public record RunResultEntry(String metric, float value, Optional<String> unit, Optional<Integer> direction) {}
