package org.leanlang.radar.server.compare;

import java.util.Optional;

public record MetricInfo(String name, Optional<String> unit, Optional<Float> quantile) {}
