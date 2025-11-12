package org.leanlang.radar.runner.supervisor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MeasurementCollector {
    public static final String OUTPUT_PREFIX = "radar::measurement=";

    private final ObjectMapper mapper;
    private final Map<String, JsonRunResultEntry> entries;

    public MeasurementCollector(ObjectMapper mapper) {
        this.mapper = mapper;
        this.entries = new HashMap<>();
    }

    public void add(JsonRunResultEntry entry) {
        JsonRunResultEntry existing = entries.get(entry.metric());
        if (existing != null) {
            entry = new JsonRunResultEntry(entry.metric(), entry.value() + existing.value(), entry.unit());
        }
        entries.put(entry.metric(), entry);
    }

    public boolean addLineFromOutput(String line) {
        if (!line.startsWith(OUTPUT_PREFIX)) return false;
        String json = line.substring(OUTPUT_PREFIX.length()).strip();

        JsonRunResultEntry entry;
        try {
            entry = mapper.readValue(json, JsonRunResultEntry.class);
        } catch (JsonProcessingException e) {
            // Ignore syntax/format errors.
            // It feels weird if the potentially arbitrary output of random programs can stop a run.
            // The user can check via the logs in the UI whether their measurements were recorded.
            return false;
        }

        add(entry);
        return true;
    }

    public void addLineFromResultFile(String line) throws Exception {
        add(mapper.readValue(line.strip(), JsonRunResultEntry.class));
    }

    public List<JsonRunResultEntry> entries() {
        return entries.values().stream().toList();
    }
}
