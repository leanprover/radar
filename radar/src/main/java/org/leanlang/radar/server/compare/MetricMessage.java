package org.leanlang.radar.server.compare;

import java.util.List;

public record MetricMessage(MetricSignificance significance, List<JsonMessageSegment> message) {}
