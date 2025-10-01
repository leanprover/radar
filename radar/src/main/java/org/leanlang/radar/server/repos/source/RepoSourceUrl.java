package org.leanlang.radar.server.repos.source;

import java.net.URI;

public record RepoSourceUrl(URI url) implements RepoSource {
    @Override
    public URI linkUrl() {
        return url;
    }

    @Override
    public URI gitUrl() {
        return url;
    }
}
