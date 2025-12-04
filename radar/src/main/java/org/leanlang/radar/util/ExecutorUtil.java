package org.leanlang.radar.util;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ExecutorUtil {
    private static final Logger log = LoggerFactory.getLogger(ExecutorUtil.class);

    private ExecutorUtil() {}

    public static void scheduleAtMidnight(
            ScheduledExecutorService executor, String task, Runnable command, Duration delay) {

        long secondsPerDay = 24 * 60 * 60;
        long secondsElapsedToday =
                Instant.now().atZone(ZoneId.systemDefault()).toLocalTime().toSecondOfDay();
        long secondsToWait = (secondsPerDay + delay.toSeconds() - secondsElapsedToday) % secondsPerDay;

        log.info("{} in {}", task, new Formatter().formatValueWithUnit(secondsToWait, "s"));
        executor.schedule(
                () -> executor.scheduleAtFixedRate(command, 0, secondsPerDay, TimeUnit.SECONDS),
                secondsToWait,
                TimeUnit.SECONDS);
    }
}
