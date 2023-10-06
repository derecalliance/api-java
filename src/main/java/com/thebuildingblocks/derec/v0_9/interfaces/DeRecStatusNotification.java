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

    /**
     * The pairable, if any, that the udpdate refers to
     */
    Optional<DeRecPairable> getPairable();

    /**
     * The secret this update refers to
     */
    DeRecSecret getSecret();

    enum Type {
        UPDATE_PROGRESS(false),
        UPDATE_AVAILABLE(false), // a sufficient number of acknowledgements have been received for an update to consider it recoverable
        UPDATE_FAILED(true),
        UPDATE_COMPLETE(false), // all update requests have been replied to, or failed
        VERIFY_PROGRESS(false),
        VERIFY_AVAILABLE(false), // a sufficient number of acknowledgements have been received for verify
        VERIFY_FAILED(true),
        VERIFY_COMPLETE(false), // all update requests have been replied to, or failed
        HELPER_NOT_PAIRED(true), // pairing failed
        HELPER_INACTIVE(true), // a previously active helper has become inactive
        HELPER_READY(false), // a helper has become active
        HELPER_UNPAIRED(false), // an unpair action successfully completed for this helper
        SECRET_UNAVAILABLE(true), // a secret that had previously been usable is now not usable
        SECRET_AVAILABLE(false); // a secret is now available for use, i.e. a sufficient number of helpers can
        // receive updates and support recovery

        public boolean isError() {
            return error;
        }

        final boolean error;

        Type(boolean error){
            this.error = error;
        }

    }
}
