package org.leanlang.radar.server.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.file.Path;

public final class ServerConfigDirs {
    @JsonProperty(required = true)
    public Path state = Path.of("state");

    @JsonProperty(required = true)
    public Path cache = Path.of("cache");
}
