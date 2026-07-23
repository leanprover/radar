package org.leanlang.radar.runner.supervisor;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Optional;

/**
 * All information required for a runner to execute and report a single run.
 */
public record JsonJob(
        @JsonProperty(required = true) String repo,
        @JsonProperty(required = true) URI url,
        Optional<String> subdir,
        @JsonProperty(required = true) String chash,
        @JsonProperty(required = true) URI benchUrl,
        @JsonProperty(required = true) String benchChash,
        @JsonProperty(required = true) String name,
        @JsonProperty(required = true) String script) {}
