package org.leanlang.radar.server.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.checkerframework.checker.nonempty.qual.NonEmpty;

public record ServerConfigRepo(
        @NotEmpty String name,
        @NotEmpty String description,
        @NotNull URI url,
        @NotEmpty Set<@NonEmpty String> track,
        @NotNull URI benchUrl,
        @NotEmpty String benchRef,
        @NotEmpty List<ServerConfigRepoRun> benchRuns) {

    private static String refify(String refName) {
        if (refName.startsWith("ref/")) return refName;
        return "refs/heads/" + refName;
    }

    public Set<String> track() {
        return track.stream().map(ServerConfigRepo::refify).collect(Collectors.toUnmodifiableSet());
    }

    public String benchRef() {
        return refify(benchRef);
    }
}
