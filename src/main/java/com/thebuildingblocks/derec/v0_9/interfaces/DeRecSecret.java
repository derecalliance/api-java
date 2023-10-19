/*
 * Copyright (c) 2023 The Building Blocks Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thebuildingblocks.derec.v0_9.interfaces;

import java.io.Closeable;
import java.util.List;
import java.util.NavigableMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * A secret is a "Helper Controller", in other words it controls the communication with each of the
 * helpers assigned to it, and carries out updates of the secret.
 */
public interface DeRecSecret extends Closeable {

    /**
     * Add helpers to this secret and block till the outcome of adding them is known
     *
     * @param helperIds a list of helper IDs to add
     */
    void addHelpers(List<? extends DeRecHelperInfo> helperIds);

    /**
     * Add helpers to this secret asynchronously
     *
     * @param helperIds a list of futures for each of the helpers
     */
    List<CompletableFuture<? extends DeRecHelperStatus>> addHelpersAsync(List<? extends DeRecHelperInfo> helperIds);

    /**
     * List the helpers
     *
     * @return a list of helpers
     */
    List<? extends DeRecHelperStatus> getHelpers();

    /**
     * Remove each of the helperIds in the list, if a helperId in the list does not refer to a helper for this secret
     * then it is ignored. Block till each of the removals has succeeded, or failed.
     *
     * @param helperIds a list of helper IDs
     */
    void removeHelpers(List<? extends DeRecHelperInfo> helperIds);

    /**
     * Remove helpers from this secret asynchronously
     *
     * @param helperIds a list of futures for each of the helpers
     */
    List<CompletableFuture<? extends DeRecHelperStatus>> removeHelpersAsync(List<? extends DeRecHelperInfo> helperIds);

    /**
     * Update a secret synchronously blocking till the outcome (success or fail) is known.
     *
     * @param bytesToProtect the bytes of the update
     * @return the new Version
     */
    DeRecVersion update(byte[] bytesToProtect);

    /**
     * Update a secret synchronously blocking till the outcome (success or fail) is known.
     *
     * @param bytesToProtect the bytes of the update
     * @param description description of this version of the secret
     * @return the new Version
     */
    DeRecVersion update(byte[] bytesToProtect, String description);

    /**
     * Update a secret asynchronously, cancelling any in-progress updates
     *
     * @param bytesToProtect the bytes of the update
     * @return a Future which completes when the update is safe or when it is known to have failed
     */
    Future<? extends DeRecVersion> updateAsync(byte[] bytesToProtect);

    /**
     * Update a secret asynchronously, cancelling any in-progress updates
     *
     * @param bytesToProtect the bytes of the update
     * @param description description of this version of the secret
     * @return a Future which completes when the update is safe or when it is known to have failed
     */
    Future<? extends DeRecVersion> updateAsync(byte[] bytesToProtect, String description);

    /**
     * The unique id of the secret
     *
     * @return the id - 1 to 16 bytes that uniquely identify this secret for this sharer
     */
    byte[] getSecretId();

    /**
     * get a list of versions of the secret
     *
     * @return a {@link NavigableMap} of versions
     */
    NavigableMap<Integer, ? extends DeRecVersion> getVersions();

    /**
     * Is the secret in a state where updates can safely be made and if it is not closed
     *
     * @return true if it is safe
     */
    boolean isAvailable();

    /**
     * Is the secret shut down
     *
     * @return true if it is shut down
     */
    boolean isClosed();

    /**
     * Close the secret asynchronously.
     */
    CompletableFuture<? extends DeRecSecret> closeAsync();

    /**
     * Gracefully shut down the secret, i.e. unpair from all helpers. Blocks till complete.
     */
    @Override
    void close();
}
