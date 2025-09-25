package org.leanlang.radar;

import java.time.Duration;

public class Constants {
    private Constants() {}

    /**
     * Timeout for all runner HTTP requests.
     */
    public static final Duration RUNNER_HTTP_REQUEST_TIMEOUT = Duration.ofMinutes(1);

    /**
     * Delay between unsuccessful job attempts (e.g. the server had no job, there was an exception, ...).
     */
    public static final Duration RUNNER_GET_JOB_DELAY = Duration.ofSeconds(10);

    /**
     * How long to wait in-between attempts to submit the results of a bench run.
     */
    public static final Duration RUNNER_SUBMIT_RESULT_DELAY = Duration.ofSeconds(10);

    /**
     * Delay between subsequent status updates.
     *
     * @see #RUNNER_CONNECTED_TIME
     */
    public static final Duration RUNNER_STATUS_UPDATE_DELAY = Duration.ofSeconds(1);

    /**
     * A runner's last status update must be at most this long ago,
     * or else the runner will no longer be considered "connected".
     *
     * @see #RUNNER_STATUS_UPDATE_DELAY
     */
    public static final Duration RUNNER_CONNECTED_TIME = Duration.ofSeconds(10);

    /**
     * Delay between {@link org.leanlang.radar.server.busser.Busser Busser} runs.
     */
    public static final Duration BUSSER_DELAY = Duration.ofMinutes(1);

    /**
     * Direction used for new metrics where the bench script specified none.
     */
    public static final int DEFAULT_DIRECTION = 0;
}
