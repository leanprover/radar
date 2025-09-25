package org.leanlang.radar.runner.supervisor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutputLines {
    private static final Logger log = LoggerFactory.getLogger(OutputLines.class);

    private final List<OutputLine> lines = new ArrayList<>();

    public synchronized void add(OutputLine line) {
        lines.add(line);
        log.debug("[{}] {}", line.source(), line.line());
    }

    public void add(int source, String line) {
        add(new OutputLine(source, line));
    }

    public void add(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        sw.toString().lines().forEach(line -> add(OutputLine.INTERNAL, line));
    }

    public synchronized List<OutputLine> getAll() {
        return lines.stream().toList();
    }

    public synchronized List<OutputLine> get(int skip) {
        return lines.stream().skip(skip).toList();
    }
}
