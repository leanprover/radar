package org.leanlang.radar.runner;

import java.nio.file.Path;

public class RunnerMain {
    public RunnerMain(Path configFile) {
        System.out.println("Runner constructed");
    }

    public void run() {
        System.out.println("Runner ran");
    }
}
