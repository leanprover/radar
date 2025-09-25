package org.leanlang.radar.runner.supervisor;

import org.leanlang.radar.server.queue.Job;

public record SupervisorStatus(Job job, OutputLines lines) {}
