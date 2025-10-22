package org.leanlang.radar.runner.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.file.Path;

public final class RunnerConfigDirs {
    @JsonProperty(required = true)
    public Path cache = Path.of("cache");

    @JsonProperty(required = true)
    public Path tmp = Path.of("tmp");
}
