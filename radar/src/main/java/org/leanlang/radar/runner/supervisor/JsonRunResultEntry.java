package org.leanlang.radar.runner.supervisor;

import java.util.Optional;
import org.leanlang.radar.server.queue.RunResultEntry;

public record JsonRunResultEntry(String metric, float value, Optional<String> unit) {
    public RunResultEntry toRunResultEntry() {
        return new RunResultEntry(metric, value, unit);
    }
}
