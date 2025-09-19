package org.leanlang.radar.runner.config;

import jakarta.validation.constraints.NotNull;
import java.nio.file.Path;

public class RunnerConfigDirs {
    @NotNull
    public Path cache = Path.of("cache");

    @NotNull
    public Path tmp = Path.of("tmp");
}
