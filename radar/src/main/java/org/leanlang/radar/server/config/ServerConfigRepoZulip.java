package org.leanlang.radar.server.config;

import org.jspecify.annotations.Nullable;

public record ServerConfigRepoZulip(
        @Nullable String feedChannel, @Nullable String feedTopic, @Nullable String linkifier) {}
