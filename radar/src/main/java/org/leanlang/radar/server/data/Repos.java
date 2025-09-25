package org.leanlang.radar.server.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.lifecycle.Managed;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.leanlang.radar.server.config.Dirs;
import org.leanlang.radar.server.config.ServerConfigRepo;

public final class Repos implements Managed {
    private final List<String> repoNames;
    private final Map<String, Repo> repos;

    public Repos(ObjectMapper mapper, Dirs dirs, List<ServerConfigRepo> repoList) throws IOException {
        repoNames = new ArrayList<>();
        repos = new HashMap<>();
        for (ServerConfigRepo repo : repoList) {
            repoNames.add(repo.name());
            repos.put(repo.name(), new Repo(mapper, dirs, repo));
        }
    }

    @Override
    public void stop() {
        for (Repo repo : repos.values()) {
            repo.close();
        }
    }

    public List<Repo> repos() {
        return repoNames.stream().map(repos::get).toList();
    }

    public Repo repo(String name) {
        var repo = repos.get(name);
        if (repo == null) {
            throw new IllegalArgumentException("No repo named " + name);
        }
        return repo;
    }
}
