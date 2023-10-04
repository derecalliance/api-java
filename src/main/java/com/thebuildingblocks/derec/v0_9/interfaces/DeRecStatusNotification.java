package com.thebuildingblocks.derec.v0_9.interfaces;

import java.util.Optional;

/**
 * A status notification may be emitted by a secret asynchronously to alert
 * an API user of changes to the status of the Secret
 */
public interface DeRecStatusNotification {
    /**
     * The type of the notification
     */
    Type getType();

    /**
     * A message describing the nature of the notification
     */
    String getMessage();

    /**
     * The version, if any, that the update refers to
     */
    Optional<DeRecVersion> getVersion();

    DeRecPairable getPairable();

    /**
     * The secret this update refers to
     */
    DeRecSecret getSecret();

    enum Type {
        UPDATE_AVAILABLE, // a sufficient number of acknowledgements have been received for an update to consider it recoverable
        UPDATE_FINISHED, // all update requests have been replied to, or failed
        HELPER_NOT_PAIRED, // pairing failed
        HELPER_INACTIVE, // a previously active helper has become inactive
        HELPER_READY, // a helper has become active
        SECRET_UNAVAILABLE, // a secret that had previously been usable is now not usable
        SECRET_AVAILABLE; // a secret is now available for use, i.e. a sufficient number of helpers can
        // receive updates and support recovery
    }
}
