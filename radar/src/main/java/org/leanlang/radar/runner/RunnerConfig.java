package org.leanlang.radar.runner;

import io.dropwizard.logging.common.LoggingFactory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import org.jspecify.annotations.Nullable;

public record RunnerConfig(
        @Valid @Nullable LoggingFactory logging,
        @NotEmpty String name,
        @NotNull URI url,
        @NotEmpty String token,
        @Valid @NotNull RunnerConfigDirs dirs) {

    URI apiUrl(String path) {
        if (path.startsWith("/")) path = path.substring(1);
        return url.resolve(path);
    }
}
