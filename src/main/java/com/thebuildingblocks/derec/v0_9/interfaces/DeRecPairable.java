package com.thebuildingblocks.derec.v0_9.interfaces;

/**
 * Something that can be paired with, a helper typically, which has a status resulting from
 * an attempt to pair and an ongoing status reflecting the secret update and pairing life cycle.
 */
public interface DeRecPairable {
    DeRecId getId();

    PairingStatus getStatus();

    enum PairingStatus {
        NONE, // not yet invited
        INVITED, // no reply yet
        PAIRED, // replied positively
        REFUSED, // replied negatively
        PENDING_REMOVAL, // in the process of being removed
        REMOVED, // at sharer request
        FAILED, // timeout, disconnect etc.
        GONE // disconnected at Helper Request
    }
}
