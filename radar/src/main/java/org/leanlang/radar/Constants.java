package org.leanlang.radar;

import java.time.Duration;

public final class Constants {
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
     * How many lines of logs to send as part of the status update if logs are available.
     */
    public static final int RUNNER_STATUS_UPDATE_LINES = 100;

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
     * Queue priority of commits added because they newly appeared on one of the tracked branches.
     */
    public static final int PRIORITY_NEW_COMMIT = 0;

    /**
     * Queue priority of commits added because of a GitHub command.
     */
    public static final int PRIORITY_GITHUB_COMMAND = 1;

    /**
     * How many times to try to post or update a comment before giving up.
     * This is quite a high value because GitHub may be down for a while.
     * It is not infinite because the bot may not have permissions to post comments
     * or the issue may have been deleted.
     */
    public static final int GITHUB_MAX_TRIES = 100;
}
