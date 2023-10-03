package com.thebuildingblocks.derec.v0_9.interfaces;

import java.io.Closeable;
import java.util.List;
import java.util.NavigableMap;
import java.util.UUID;
import java.util.concurrent.Future;

/**
 * A secret is a "Helper Controller", in other words it controls the communication with each of the
 * helpers assigned to it, and carries out updates of the secret.
 */
public interface DeRecSecret extends Closeable {

    /**
     * Add helpers to this secret and block till the outcome of adding them is known
     * @param helperIds a list of helper IDs to add
     */
    void addHelpers(List<? extends DeRecId> helperIds);

    /**
     * Add helpers to this secret asynchronously
     * @param helperIds a list of futures for each of the helpers
     */
    List<Future<? extends DeRecPairable>> addHelpersAsync(List<? extends DeRecId> helperIds);

    /**
     * List the helpers
     * @return a list of helpers
     */
    List<? extends DeRecPairable> getHelpers();

    /**
     * Remove each of the helperIds in the list, if a helperId in the list does not refer to a helper for this secret
     * then it is ignored. Block till each of the removals has succeeded, or failed.
     * @param helperIds a list of helper IDs
     */
    void removeHelpers(List<? extends DeRecId> helperIds);

    /**
     * Update a secret synchronously blocking till the outcome (success or fail) is known.
     * @param bytesToProtect the bytes of the update
     * @return the new Version
     */
    DeRecVersion update(byte[] bytesToProtect);

    /**
     * Update a secret asynchronously, cancelling any in-progress updates
     * @param bytesToProtect the bytes of the update
     * @return a Future which completes when the update is safe or when it is known to have failed
     */
    Future<? extends DeRecVersion> updateAsync(byte[] bytesToProtect);

    /**
     * get a list of versions of the secret
     * @return a List of versions
     */
    NavigableMap<Integer, ? extends DeRecVersion> getVersions();
    /**
     * Is the secret in a state where updates can safely be made and if it is not closed
     * @return true if it is safe
     */
    boolean isAvailable();

    /**
     * Is the secret shut down
     * @return true if it is shut down
     */
    boolean isClosed();
    /**
     * The unique id of the secret
     * @return the id
     */
    UUID getSecretId();

    /**
     * A secret has a human-readable description as a memo for what the secret is for etc.
     * @return the description
     */
    String getDescription();
    /**
     * Gracefully shut down the secret, i.e. unpair from all helpers.
     */
    @Override
    void close();


}
