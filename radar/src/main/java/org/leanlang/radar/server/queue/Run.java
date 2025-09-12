package org.leanlang.radar.server.queue;

import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.server.runners.Runner;

public final class Run {
    private final Runner runner;
    private final String script;
    private boolean active;
    private @Nullable RunResult result;

    public Run(Runner runner, String script) {
        this.runner = runner;
        this.script = script;
        this.active = false;
        this.result = null;
    }

    public Runner runner() {
        return runner;
    }

    public String script() {
        return script;
    }

    public boolean active() {
        return active;
    }

    public Optional<RunResult> result() {
        return Optional.ofNullable(result);
    }
}
