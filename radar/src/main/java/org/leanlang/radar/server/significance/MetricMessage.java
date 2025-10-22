package org.leanlang.radar.server.significance;

import java.util.List;

public record MetricMessage(MetricSignificance significance, List<JsonMessageSegment> message) {}
