package org.derecalliance.derec.api;


/**
 * Representation of a sharer as perceived by a helper
 */
interface DeRecSharerStatus extends DeRecPairingStatus {
    DeRecIdentity getId();

    /**
     * Returns whether the sharer is in recovery mode
     *
     * @return true if sharer is in recovery mode, false otherwise
     */
    boolean isRecovering();
}
