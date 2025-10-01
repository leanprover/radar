package org.leanlang.radar.server.repos.source;

import java.net.URI;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record RepoSourceGithub(String owner, String repo) implements RepoSource {
    static Optional<RepoSourceGithub> parse(URI url) {
        // Example URLs:
        // https://github.com/leanprover/radar
        // https://github.com/leanprover/radar.git
        // git@github.com:leanprover/radar.git
        Pattern pattern =
                Pattern.compile("(https?://github\\.com/|git@github\\.com:)(?<owner>[^/]+)/(?<repo>[^/]+)(/|\\.git)?");

        Matcher matcher = pattern.matcher(url.toString());
        if (!matcher.matches()) return Optional.empty();
        return Optional.of(new RepoSourceGithub(matcher.group("owner"), matcher.group("repo")));
    }

    public String ownerAndRepo() {
        return owner + "/" + repo;
    }

    @Override
    public URI linkUrl() {
        return URI.create("https://github.com/" + ownerAndRepo());
    }

    @Override
    public URI gitUrl() {
        return URI.create("https://github.com/" + owner + "/" + repo + ".git");
    }
}
