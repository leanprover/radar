package org.leanlang.radar.runner.supervisor;

import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.server.queue.RunResultEntry;

public final class JsonRunResultEntry {
    @NotNull
    public String metric;

    @NotNull
    public float value;

    @Nullable
    public String unit;

    @Nullable
    public Integer direction;

    public RunResultEntry toRunResultEntry() {
        return new RunResultEntry(metric, value, Optional.ofNullable(unit), Optional.ofNullable(direction));
    }
}
