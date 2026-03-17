package org.leanlang.radar.server.compare;

import java.time.Instant;
import java.util.Optional;

public record MetricInfo(String name, Optional<String> unit, Instant firstSeen, Optional<Float> quantile) {}
