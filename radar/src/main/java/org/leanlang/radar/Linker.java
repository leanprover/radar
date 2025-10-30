package org.leanlang.radar;

import java.net.URI;

public record Linker(URI url) {
    public URI linkToCommit(String repo, String chash) {
        return url.resolve("/repos/" + repo + "/commits/" + chash);
    }

    public URI linkToComparison(String repo, String first, String second) {
        return url.resolve("/repos/" + repo + "/commits/" + second + "?parent=" + first);
    }
}
