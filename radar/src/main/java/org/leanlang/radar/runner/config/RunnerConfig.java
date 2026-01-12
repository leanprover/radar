package org.leanlang.radar.runner.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.logging.common.LoggingFactory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.net.URI;
import org.jspecify.annotations.Nullable;

public final class RunnerConfig {
    @Valid
    @Nullable
    public LoggingFactory logging;

    @NotEmpty
    @JsonProperty(required = true)
    public String name;

    @JsonProperty(required = true)
    public URI url;

    @NotEmpty
    @JsonProperty(required = true)
    public String token;

    @Nullable
    public String systemConfigurationId;

    @Valid
    @JsonProperty(required = true)
    public RunnerConfigDirs dirs = new RunnerConfigDirs();

    public URI apiUrl(String path) {
        if (path.startsWith("/")) path = path.substring(1);
        return url.resolve(path);
    }
}
