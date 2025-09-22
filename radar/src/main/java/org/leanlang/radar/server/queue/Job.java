package org.leanlang.radar.server.queue;

import java.net.URI;

public record Job(String repo, URI url, String chash, URI benchUrl, String benchChash, String name, String script) {}
