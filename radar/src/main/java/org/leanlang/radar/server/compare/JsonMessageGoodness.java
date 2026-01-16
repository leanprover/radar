package org.leanlang.radar.server.compare;

public enum JsonMessageGoodness {
    GOOD,
    NEUTRAL,
    BAD;

    public static JsonMessageGoodness fromDelta(float delta, int direction) {
        if (delta == 0 || direction == 0) return NEUTRAL;
        if ((delta > 0) == (direction > 0)) return GOOD;
        return BAD;
    }
}
