package org.leanlang.radar.util;

import java.net.URI;

public record GithubLinker(String owner, String repo) {
    public static final URI BASE = URI.create("https://github.com/");

    public URI label(String label) {
        return BASE.resolve(owner + "/" + repo + "/labels/" + label);
    }

    public URI commit(String chash) {
        return BASE.resolve(owner + "/" + repo + "/commit/" + chash);
    }
}
