package org.leanlang.radar.server.config;

import jakarta.validation.constraints.NotNull;
import java.nio.file.Path;

public class ServerConfigDirs {
    @NotNull
    public Path state = Path.of("state");

    @NotNull
    public Path cache = Path.of("cache");
}
