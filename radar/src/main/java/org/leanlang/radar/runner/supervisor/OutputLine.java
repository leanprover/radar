package org.leanlang.radar.runner.supervisor;

import java.time.Instant;

public record OutputLine(Instant time, int source, String line) {
    public static final int STDOUT = 0;
    public static final int STDERR = 1;
    public static final int INTERNAL = 2;

    public OutputLine(int source, String line) {
        this(Instant.now(), source, line);
    }
}
