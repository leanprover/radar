package org.leanlang.radar.util;

import java.net.URI;

public record RadarLinker(URI base) {
    public URI commit(String repo, String chash) {
        return base.resolve("/repos/" + repo + "/commits/" + chash);
    }

    public URI comparison(String repo, String first, String second) {
        return base.resolve("/repos/" + repo + "/commits/" + second + "?reference=" + first);
    }
}
