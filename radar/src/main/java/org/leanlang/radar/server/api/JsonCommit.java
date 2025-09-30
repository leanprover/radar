package org.leanlang.radar.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Optional;
import org.leanlang.radar.codegen.jooq.tables.records.CommitsRecord;

public record JsonCommit(
        @JsonProperty(required = true) String chash,
        @JsonProperty(required = true) Ident author,
        @JsonProperty(required = true) Ident committer,
        @JsonProperty(required = true) String title,
        Optional<String> body) {

    public record Ident(
            @JsonProperty(required = true) String name,
            @JsonProperty(required = true) String email,
            @JsonProperty(required = true) Instant time,
            @JsonProperty(required = true) int offset) {}

    public JsonCommit(CommitsRecord record) {
        this(
                record.getChash(),
                new Ident(
                        record.getAuthorName(),
                        record.getAuthorEmail(),
                        record.getAuthorTime(),
                        record.getAuthorOffset()),
                new Ident(
                        record.getCommitterName(),
                        record.getCommitterEmail(),
                        record.getCommitterTime(),
                        record.getCommitterOffset()),
                record.getMessageTitle(),
                Optional.ofNullable(record.getMessageBody()));
    }
}
