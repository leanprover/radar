package org.leanlang.radar.server.compare;

import java.util.List;

public record JsonSignificance(boolean major, List<JsonMessageSegment> message) {}
