package org.leanlang.radar.server.queue;

public record RunId(String repo, String chash, String runner, String script) {}
