package org.leanlang.radar.server.config;

import jakarta.validation.constraints.NotEmpty;

public record ServerConfigRunner(@NotEmpty String name, @NotEmpty String token) {}
