package com.thebuildingblocks.derec.model;

import java.net.URI;

public class Util{
    /**
     * Some formal system for identification of parties, does this need to be the same across all implementations
     */
    public static class DeRecId {
        URI contact; // how to contact me outside of the protocol, an email address, for example
        // some kind of unique id ...
    }

    /**
     * Placeholder for some way of counting retries
     */
    public static class RetryStatus {

    }

    /**
     * Placeholder for some way of describing and agreeing retry parameters
     */
    public static class RetryParameters {
        // recording number of retries
        // re-verification delay
        // time to wait before share update
    }
}