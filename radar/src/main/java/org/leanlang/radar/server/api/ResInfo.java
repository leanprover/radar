package org.leanlang.radar.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.net.URI;
import java.util.List;
import org.leanlang.radar.server.config.ServerConfigLegalLink;

@Path("/info/")
public record ResInfo(List<ServerConfigLegalLink> legalLinks) {

    public record JsonLegalLink(
            @JsonProperty(required = true) String name,
            @JsonProperty(required = true) URI url) {}

    public record JsonGet(@JsonProperty(required = true) List<JsonLegalLink> legalLinks) {}

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonGet get() {
        List<JsonLegalLink> links = legalLinks.stream()
                .map(it -> new JsonLegalLink(it.name(), it.url()))
                .toList();
        return new JsonGet(links);
    }
}
