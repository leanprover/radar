package org.leanlang.radar.server.config;

import io.dropwizard.core.Configuration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class RadarConfig extends Configuration {

    @Valid
    @NotNull
    public RadarConfigDirs dirs;

    @Valid
    @NotEmpty
    @RepoNamesUnique
    public List<RadarConfigRepo> repos;
}
