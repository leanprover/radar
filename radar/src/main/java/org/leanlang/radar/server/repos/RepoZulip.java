package org.leanlang.radar.server.repos;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import java.util.Base64;
import org.leanlang.radar.server.config.ServerConfigRepoZulip;
import org.leanlang.radar.server.config.credentials.ZulipCredentials;

public record RepoZulip(Client client, ServerConfigRepoZulip config, ZulipCredentials credentials) {
    private MultivaluedMap<String, Object> headers() {
        MultivaluedHashMap<String, Object> result = new MultivaluedHashMap<>();

        String authPlain = credentials.email() + ":" + credentials.key();
        String authEncoded = Base64.getEncoder().encodeToString(authPlain.getBytes());
        result.add(HttpHeaders.AUTHORIZATION, "Basic " + authEncoded);

        return result;
    }

    // https://zulip.com/api/send-message
    public void sendMessage(String channel, String topic, String content) {
        Form form = new Form()
                .param("type", "stream")
                .param("to", channel)
                .param("topic", topic)
                .param("content", content);

        try (Response response = client.target(credentials.site())
                .path("api")
                .path("v1")
                .path("messages")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .headers(headers())
                .post(Entity.form(form))) {

            if (response.getStatus() != 200) {
                throw new RuntimeException("Failed to post zulip message, status code " + response.getStatus());
            }
        }
    }
}
