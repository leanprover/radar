package org.leanlang.radar.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.core.Configuration;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class RadarConfiguration extends Configuration {
    @NotEmpty
    private String debug;

    // TODO Ensure somehow that repo names are unique
    @NotEmpty
    private List<RepoConfig> repos;

    @JsonProperty
    public String getDebug() {
        return debug;
    }

    @JsonProperty
    public void setDebug(String debug) {
        this.debug = debug;
    }

    @JsonProperty
    public List<RepoConfig> getRepos() {
        return repos;
    }

    @JsonProperty
    public void setRepos(List<RepoConfig> repos) {
        this.repos = repos;
    }
}
