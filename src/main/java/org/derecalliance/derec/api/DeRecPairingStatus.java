package org.derecalliance.derec.api;

public interface DeRecPairingStatus {
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
