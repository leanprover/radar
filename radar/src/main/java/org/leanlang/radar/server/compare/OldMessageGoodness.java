package org.leanlang.radar.server.compare;

// TODO Remove once old significance computation is obsolete
public enum OldMessageGoodness {
    GOOD,
    NEUTRAL,
    BAD;

    public static OldMessageGoodness fromDelta(float delta, int direction) {
        if (delta == 0 || direction == 0) return NEUTRAL;
        if ((delta > 0) == (direction > 0)) return GOOD;
        return BAD;
    }

    public int toInt() {
        return switch (this) {
            case GOOD -> 1;
            case NEUTRAL -> 0;
            case BAD -> -1;
        };
    }
}
