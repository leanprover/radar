package org.leanlang.radar.server.config.credentials;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record ZulipCredentials(String site, String email, String key) {
    @Override
    public String toString() {
        return "ZulipCredentials{site='" + site + "', ...}";
    }

    public static ZulipCredentials load(Path file) throws IOException {
        Properties properties = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            properties.load(reader);
        }

        String site = properties.getProperty("site");
        String email = properties.getProperty("email");
        String key = properties.getProperty("key");

        if (site == null || email == null || key == null)
            throw new IllegalArgumentException("Zulip credentials at " + file + " are incomplete");

        return new ZulipCredentials(site, email, key);
    }
}
