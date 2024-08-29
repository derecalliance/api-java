package org.derecalliance.derec.api;

import java.util.List;

/**
 * Representation of a "share". The helper knows nothing about a share other than that it is some
 * binary content stored by the library (which the app has no access to) which is identified by a sharer id and
 * a secret id local to that sharer id. The app also has access to the version numbers kept by the library.
 * <p>
 * An app may remove a share, which has the effect of getting the library to mark the share as inactive and at
 * the next opportunity (on receipt of the next communication from the sharer) to request unpairing.
 */
public interface DeRecShare {
    /**
     * The sharer that this share belongs to
     */
    DeRecSharerStatus getSharer();

    /**
     * This share's secret id
     */
    DeRecSecret.Id getSecretId();

    /**
     * A list of versions currently held by the library
     */
    List<Integer> getVersions();

    /**
     * request removal of a share meaning make the share inactive and at the next opportunity, unpair from
     * the sharer for this secret.
     * The sharer's status becomes {@link DeRecHelperStatus.PairingStatus#PENDING_REMOVAL} until
     * the unpair request has been signalled to the sharer.
     * @return true if request has been carried out successfully, false if it has already been requested
     * or if the share is not known (possibly as a result of having previously been removed)
     */
    boolean remove();
}
