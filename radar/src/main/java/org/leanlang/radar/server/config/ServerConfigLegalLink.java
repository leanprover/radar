package org.leanlang.radar.server.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.net.URI;

public record ServerConfigLegalLink(
        @NotEmpty @JsonProperty(required = true) String name,
        @NotNull @JsonProperty(required = true) URI url) {}
