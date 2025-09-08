package org.leanlang.radar.server.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.net.URI;

public record ServerConfigRepo(@NotEmpty String name, @NotNull URI url, @NotEmpty String description) {}
