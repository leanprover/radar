package org.leanlang.radar.server.compare;

import java.util.List;

public record JsonMetricSignificance(boolean major, List<JsonMessageSegment> message) {}
