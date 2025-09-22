package org.leanlang.radar.server.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.nonempty.qual.NonEmpty;

public record ServerConfigRepo(
        @NotEmpty String name,
        @NotEmpty String description,
        @NotNull URI url,
        @NotEmpty Set<@NonEmpty String> track,
        @NotNull URI benchUrl,
        @NotEmpty String benchRef,
        @Valid @NotEmpty @RepoRunNamesUnique List<ServerConfigRepoRun> benchRuns) {}
