package org.leanlang.radar.server.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.core.Configuration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import org.leanlang.radar.server.config.validators.RepoNamesUnique;
import org.leanlang.radar.server.config.validators.RunnerNamesUnique;

public final class ServerConfig extends Configuration {
    @Valid
    @JsonProperty(required = true)
    public JerseyClientConfiguration jerseyClient = new JerseyClientConfiguration();

    @Valid
    @NotNull
    @JsonProperty(required = true)
    public URI url;

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
