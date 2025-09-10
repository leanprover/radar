package org.leanlang.radar.server.runners;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.leanlang.radar.server.config.ServerConfigRunner;

public final class Runners {
    private final Map<String, Runner> runners;

    public Runners(List<ServerConfigRunner> runnerList) {
        runners = runnerList.stream()
                .map(Runner::new)
                .collect(Collectors.toMap(it -> it.getConfig().name(), it -> it));
    }

    public List<Runner> getRunners() {
        return runners.values().stream().toList();
    }

    public Runner getRunner(String name) {
        var runner = runners.get(name);
        if (runner == null) {
            throw new IllegalArgumentException("No runner named " + name);
        }
        return runner;
    }

    public Runner getRunner(String name, String token) {
        var runner = getRunner(name);
        if (!runner.getConfig().token().equals(token)) {
            throw new IllegalArgumentException("Invalid token for runner " + name);
        }
        return runner;
    }
}
