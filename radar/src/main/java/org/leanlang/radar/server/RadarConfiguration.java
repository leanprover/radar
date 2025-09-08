package org.leanlang.radar.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.core.Configuration;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RadarConfiguration extends Configuration {
    private static final Logger log = LoggerFactory.getLogger(RadarConfiguration.class);

    @NotEmpty
    private List<RepoConfig> repos;

    @JsonProperty
    public List<RepoConfig> getRepos() {
        return repos;
    }

    @JsonProperty
    public void setRepos(List<RepoConfig> repos) {
        this.repos = repos;
    }

    public void validate() {
        boolean valid = true;

        // Ensure repo names are unique
        Set<String> uniqueNames = repos.stream().map(RepoConfig::name).collect(Collectors.toSet());
        if (uniqueNames.size() != repos.size()) {
            log.error("Repo names are not unique");
            valid = false;
        }

        if (!valid) {
            throw new RuntimeException("Server config is invalid");
        }
    }
}
