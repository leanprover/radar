package org.leanlang.radar.server.config;

import jakarta.validation.constraints.NotEmpty;

public record ServerConfigRepoRun(@NotEmpty String runner, @NotEmpty String script) {}
