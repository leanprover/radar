package org.leanlang.radar.server.config.credentials;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public record CredentialsByRepo(Map<String, GithubCredentials> github, Map<String, ZulipCredentials> zulip) {
    public static CredentialsByRepo load(
            @Nullable Map<String, Path> githubPaths, @Nullable Map<String, Path> zulipPaths) throws IOException {

        HashMap<String, GithubCredentials> github = new HashMap<>();
        if (githubPaths != null) {
            for (Map.Entry<String, Path> entry : githubPaths.entrySet()) {
                github.put(entry.getKey(), GithubCredentials.load(entry.getValue()));
            }
        }

        HashMap<String, ZulipCredentials> zulip = new HashMap<>();
        if (zulipPaths != null) {
            for (Map.Entry<String, Path> entry : zulipPaths.entrySet()) {
                zulip.put(entry.getKey(), ZulipCredentials.load(entry.getValue()));
            }
        }

        return new CredentialsByRepo(github, zulip);
    }
}
