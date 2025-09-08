package org.leanlang.radar.server.config;

import io.dropwizard.core.Configuration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class ServerConfig extends Configuration {

    @Valid
    @NotNull
    public ServerConfigDirs dirs;

    @Valid
    @NotEmpty
    @RepoNamesUnique
    public List<ServerConfigRepo> repos;
}
