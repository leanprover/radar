package org.leanlang.radar.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.core.Configuration;
import jakarta.validation.constraints.NotEmpty;

public class RadarConfiguration extends Configuration {
    @NotEmpty
    private String debug;

    @JsonProperty
    public String getDebug() {
        return debug;
    }

    @JsonProperty
    public void setDebug(String debug) {
        this.debug = debug;
    }
}
