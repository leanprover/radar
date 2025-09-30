package org.leanlang.radar.server.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.core.Configuration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public final class ServerConfig extends Configuration {

    @Valid
    @JsonProperty(required = true)
    public ServerConfigDirs dirs = new ServerConfigDirs();

    @NotEmpty
    @JsonProperty(required = true)
    public String adminToken;

    @Valid
    @NotEmpty
    @RepoNamesUnique
    @JsonProperty(required = true)
    public List<ServerConfigRepo> repos;

    @Valid
    @NotEmpty
    @RunnerNamesUnique
    @JsonProperty(required = true)
    public List<ServerConfigRunner> runners;
}
