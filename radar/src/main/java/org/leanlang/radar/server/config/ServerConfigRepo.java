package org.leanlang.radar.server.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.Set;
import java.util.stream.Collectors;
import org.checkerframework.checker.nonempty.qual.NonEmpty;

public record ServerConfigRepo(
        @NotEmpty String name, @NotNull URI url, @NotEmpty String description, @NotEmpty Set<@NonEmpty String> track) {

    public Set<String> track() {
        return track.stream()
                .map(it -> it.startsWith("refs/") ? it : "refs/heads/" + it)
                .collect(Collectors.toUnmodifiableSet());
    }
}
