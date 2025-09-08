package org.leanlang.radar.runner;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.net.URI;

public record RunnerConfig(
        @NotEmpty String name, @NotNull URI url, @NotEmpty String token, @Valid @NotNull RunnerConfigDirs dirs) {}
