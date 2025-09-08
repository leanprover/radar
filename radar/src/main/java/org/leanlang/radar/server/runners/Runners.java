package org.leanlang.radar.server.runners;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.leanlang.radar.server.config.ServerConfigRunner;

public class Runners {
    private final Map<String, Runner> runners;

    public Runners(List<ServerConfigRunner> runnerList) {
        runners = runnerList.stream()
                .map(Runner::new)
                .collect(Collectors.toMap(it -> it.getConfig().name(), it -> it));
    }

    public synchronized List<Runner> getRunners() {
        return runners.values().stream().toList();
    }
}
