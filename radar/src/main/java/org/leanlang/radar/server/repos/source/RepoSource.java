package org.leanlang.radar.server.repos.source;

import java.net.URI;
import java.util.Optional;

public sealed interface RepoSource permits RepoSourceGithub, RepoSourceUrl {
    static RepoSource parse(URI url) {
        Optional<RepoSourceGithub> githubOpt = RepoSourceGithub.parse(url);
        if (githubOpt.isPresent()) return githubOpt.get();

        return new RepoSourceUrl(url);
    }

    URI linkUrl();

    URI gitUrl();
}
