package org.leanlang.radar.runner.supervisor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutputLines {
    private static final Logger log = LoggerFactory.getLogger(OutputLines.class);

    private final List<JsonOutputLine> lines = new ArrayList<>();

    private synchronized void add(JsonOutputLine line) {
        lines.add(line);
        log.debug("[{}] {}", line.source(), line.line());
    }

    private void add(int source, String line) {
        add(new JsonOutputLine(Instant.now(), source, line));
    }

    public void addStdout(String line) {
        add(JsonOutputLine.STDOUT, line);
    }

    public void addStderr(String line) {
        add(JsonOutputLine.STDERR, line);
    }

    public void addInternal(String line) {
        add(JsonOutputLine.INTERNAL, line);
    }

    public void addInternal(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        sw.toString().lines().forEach(this::addInternal);
    }

    public synchronized List<JsonOutputLine> getAll() {
        return lines.stream().toList();
    }

    public synchronized List<JsonOutputLine> getLast(int n) {
        int end = lines.size();
        int start = Math.max(0, end - n);
        return List.copyOf(lines.subList(start, end));
    }
}
