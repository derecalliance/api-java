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
        int maxRetries = 0; // don't retry on failure
        Duration timeout = Duration.ofSeconds(30); // timeout if no response received
        Duration reverification = Duration.ofMinutes(30); // re-verify every
        Duration updateDelay = Duration.ofSeconds(30); // max update frequency

        public RetryParameters clone() {
            // todo
            return new RetryParameters();
        }
    };

    /**
     * Placeholder for some way of counting retries
     */
    public static class RetryStatus {

    }
}
