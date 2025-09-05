package org.leanlang.radar.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.core.Configuration;
import jakarta.validation.constraints.NotEmpty;
import java.util.Map;

public class RadarConfiguration extends Configuration {
    @NotEmpty
    private String debug;

    @NotEmpty
    private Map<String, RepoConfig> repos;

    @JsonProperty
    public String getDebug() {
        return debug;
    }

    @JsonProperty
    public void setDebug(String debug) {
        this.debug = debug;
    }

    @JsonProperty
    public Map<String, RepoConfig> getRepos() {
        return repos;
    }

    @JsonProperty
    public void setRepos(Map<String, RepoConfig> repos) {
        this.repos = repos;
    }
}
