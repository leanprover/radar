package org.leanlang.radar.server.config.credentials;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public record GithubCredentials(String personalAccessToken) {
    @Override
    public String toString() {
        return "GithubPersonalAccessToken{...}";
    }

    public static GithubCredentials load(Path file) throws IOException {
        String token = Files.readString(file).strip();
        return new GithubCredentials(token);
    }
}
