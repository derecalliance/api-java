package com.thebuildingblocks.derec.v0_9.httpprototype;


import java.time.Duration;

/**
 * placeholder for stuff needed in a few places
 */

public class Util {

    /**
     * Placeholder for some way of describing and agreeing retry parameters
     */
    public static class RetryParameters {
        public static RetryParameters DEFAULT = new RetryParameters();
        public long pairingWaitSecs = 5; // time to wait for pairings to complete or fail
        int maxRetries = 0; // don't retry on failure
        Duration timeout = Duration.ofSeconds(5); // timeout if no response received
        Duration connectTimeout = Duration.ofSeconds(5);
        Duration reverification = Duration.ofMinutes(30); // re-verify every
        Duration updateDelay = Duration.ofSeconds(30); // max update frequency

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public Duration getResponseTimeout() {
            return timeout;
        }
    };

    /**
     * Placeholder for some way of counting retries
     */
    public static class RetryStatus {

    }
}
