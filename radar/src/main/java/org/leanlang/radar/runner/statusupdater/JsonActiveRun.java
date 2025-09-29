package org.leanlang.radar.runner.statusupdater;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import org.leanlang.radar.runner.supervisor.JsonJob;
import org.leanlang.radar.runner.supervisor.JsonOutputLineBatch;

public record JsonActiveRun(
        @JsonProperty(required = true) JsonJob job,
        @JsonProperty(required = true) Instant startTime,
        @JsonProperty(required = true) JsonOutputLineBatch lines) {}
