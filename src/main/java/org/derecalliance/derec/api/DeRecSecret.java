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

package org.derecalliance.derec.api;

import java.io.Closeable;
import java.util.Arrays;
import java.util.List;
import java.util.NavigableMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * A secret is a "Helper Controller", in other words it controls the communication with each of the
 * helpers assigned to it, and carries out updates of the secret.
 *
 * <p>
 * Example usage:
 * <pre>{@code
 * secret = sharer.newSecret(bytesToProtect) // creates version 0, no helpers, not shared
 * secret.addHelpers(some helpers) // pairs helpers, not shared yet
 * secret.update() // gets sent to all paired helpers - version 1
 * secret.addHelpers( ...) // adds helpers to the secret but doesn't share
 * secret.removeHelpers( ...) // removes helpers, unpairs
 * secret.update() // gets sent to current paired helpers - version 2
 * }</pre>
 */
public interface DeRecSecret extends Closeable {

    /**
     * SecretId is a byte array between 1 and 16 bytes, we wrap it to ensure validity and to make it
     * possible to use it as a key for a {@link java.util.Map}
     */
    class Id {
        private byte[] bytes;

        public Id(byte[] bytes) {
            setBytes(bytes);
        }

        public byte[] getBytes() {
            return bytes;
        }
        public void setBytes(byte[] bytes){
            if (bytes.length < 1 || bytes.length > 16) {
                throw new IllegalArgumentException("Secret Id must be between 1 and 16 bytes");
            }
            this.bytes = Arrays.copyOf(bytes, bytes.length);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Id id)) return false;
            return Arrays.equals(bytes, id.bytes);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(bytes);
        }
    }

    /**
     * Add helpers to this secret and block till the outcome of adding them is known.
     * This does not send them shares.
     * The app must call an update method after adding the helpers.
     *
     * @param helperIds a list of helper IDs to add
     */
    void addHelpers(List<? extends DeRecIdentity> helperIds);

    /**
     * Add helpers to this secret asynchronously, but do not send them shares.
     * The app must call an update method after adding the helpers.
     *
     * @param helperIds a list of futures for each of the helpers
     */
    List<CompletableFuture<? extends DeRecHelperStatus>> addHelpersAsync(List<? extends DeRecIdentity> helperIds);

    /**
     * List the helpers
     *
     * @return a list of helpers
     */
    List<? extends DeRecHelperStatus> getHelperStatuses();

    /**
     * Remove each of the helperIds in the list, if a helperId in the list does not refer to a helper for this secret
     * then it is ignored. Block till each of the removals has succeeded, or failed.
     *
     * @param helperIds a list of helper IDs
     */
    void removeHelpers(List<? extends DeRecIdentity> helperIds);

    /**
     * Remove helpers from this secret asynchronously
     *
     * @param helperIds a list of futures for each of the helpers
     */
    List<CompletableFuture<? extends DeRecHelperStatus>> removeHelpersAsync(List<? extends DeRecIdentity> helperIds);

    /**
     * Update a secret synchronously blocking till the outcome (success or fail) is known, success
     * or failure being measured by the update being acknowledged by a threshold number of helpers
     * or it being impossible to achieve that threshold ... rather than a response having been received
     * from all helpers
     * <p>
     * The list of currently paired helpers is used. This version of the method being intended to allow
     * re-share amongst a different constituency of helpers than the previous version.
     * <p>
     * Any previous update that is not complete is cancelled.
     * <p>
     * The values of {@code bytesToProtect} and {@code description} are kept from the current latest version at the
     * time of the call.
     *
     * @return a new {@link DeRecVersion}
     */
    DeRecVersion update();

    /**
     * Update the secret with a new value, the operation being carried out as described for {@link #update()}
     * @see #update()
     * @param bytesToProtect the bytes of the update
     * @return the new Version
     */
    DeRecVersion update(byte[] bytesToProtect);

    /**
     * Update the secret with a new value, and a new description, the operation being carried out as
     * described for {@link #update()}
     * @see #update()
     * @param bytesToProtect the bytes of the update
     * @param description description of this version of the secret
     * @return the new Version
     */
    DeRecVersion update(byte[] bytesToProtect, String description);

    /**
     * Update a secret asynchronously, cancelling any in-progress updates, using the previous values
     * of {@code bytesToProtect} and {@code description}, amongst the currently paired helpers for this secret.
     *
     * @return a Future which completes when the update is safe or when it is known to have failed
     */
    Future<? extends DeRecVersion> updateAsync();


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
    Id getSecretId();

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
