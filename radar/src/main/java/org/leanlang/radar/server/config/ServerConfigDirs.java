package org.leanlang.radar.server.config;

import jakarta.validation.constraints.NotNull;
import java.nio.file.Path;

public record ServerConfigDirs(@NotNull Path data, @NotNull Path cache, @NotNull Path tmp) {}
