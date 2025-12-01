package org.leanlang.radar.server.repos.github;

import com.fasterxml.jackson.annotation.JsonProperty;

public record JsonGhUser(
        @JsonProperty(required = true) long id,
        @JsonProperty(required = true) String login) {}
