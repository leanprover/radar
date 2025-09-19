package org.leanlang.radar.runner.config;

import io.dropwizard.logging.common.LoggingFactory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import org.jspecify.annotations.Nullable;

public class RunnerConfig {
    @Valid
    @Nullable
    public LoggingFactory logging;

    @NotEmpty
    public String name;

    @NotNull
    public URI url;

    @NotEmpty
    public String token;

    @Valid
    @NotNull
    public RunnerConfigDirs dirs = new RunnerConfigDirs();

    public URI apiUrl(String path) {
        if (path.startsWith("/")) path = path.substring(1);
        return url.resolve(path);
    }
}
