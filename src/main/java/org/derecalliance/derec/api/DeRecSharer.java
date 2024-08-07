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

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * A factory for and container of Secrets in this API
 * <p>
 * Note: this interface is currently the subject of debate, especially regarding
 * the presence of the parameter "recovery" on each of the {@code newSecret} methods
 */
public interface DeRecSharer {

    /**
     * Create a new secret and auto-allocate its ID. Block till pairing concludes.
     * This secreet will be shared with the given helpers.
     *
     * @param description    a human readable description
     * @param bytesToProtect the content of the secret
     * @param helperIds      the ids of helpers for this secret
     * @param recovery       should this start in recovery mode? (i.e., each pairing happens with that flag set)
     * @return a secret
     */
    DeRecSecret newSecret(String description, byte[] bytesToProtect, List<DeRecIdentity> helperIds,
                          boolean recovery);

    /**
     * Create a new secret. Block till pairing concludes.
     * This secret will be shared with the given helpers.
     *
     * @param secretId       1 to 16 bytes that uniquely identify this secret for this sharer
     * @param description    a human readable description
     * @param bytesToProtect the content of the secret
     * @param helperIds      the ids of helpers for this secret
     * @param recovery       should this start in recovery mode? (i.e., each pairing happens with that flag set)
     * @return a secret
     */
    DeRecSecret newSecret(DeRecSecret.Id secretId, String description, byte[] bytesToProtect,
                          List<DeRecIdentity> helperIds, boolean recovery);

    /**
     * Create a new secret for later addition of helpers. AutoAllocate its ID.
     * This secret will not be shared with any helpers until an update method is called.
     *
     * @param description    a human readable description
     * @param bytesToProtect the content of the secret
     * @param recovery       should this be in recovery mode? (i.e., each pairing happens with that flag set)
     * @return a secret
     */
    DeRecSecret newSecret(String description, byte[] bytesToProtect, boolean recovery);

    /**
     * Create a new secret for later addition of helpers.
     *
     * @param secretId       1 to 16 bytes that uniquely identify this secret for this sharer
     * @param description    a human readable description
     * @param bytesToProtect the content of the secret
     * @param recovery       should this be in recovery mode? (i.e., each pairing happens with that flag set)
     * @return a secret
     */
    DeRecSecret newSecret(DeRecSecret.Id secretId, String description, byte[] bytesToProtect, boolean recovery);

    /**
     * Get the secret with this UUID, return null if none with this ID
     *
     * @param secretId a secret ID
     * @return a secret or null
     */
    DeRecSecret getSecret(DeRecSecret.Id secretId);

    /**
     * Get a list of all secrets known to this sharer
     *
     * @return a list
     */
    List<? extends DeRecSecret> getSecrets();

    /**
     * Get a list of the secrets held by a helper
     * @param helper the helper to provide the list
     * @return a map of secretId to version numbers known by this helper
     */
    Future<Map<DeRecSecret.Id, List<Integer>>> getSecretIdsAsync(DeRecIdentity helper);

    /**
     * Reconstruct a secret from a list of helpers, block till the recovery is complete.
     * This is not normally needed.  Normal recovery is done by creating a new secret with
     * recovery==true, and then the library will automatically recover as soon as a sufficient
     * number of helpers have been paired with. But this method can be used in the unusual case
     * where it is desired to recover a secret whose secret ID is already known, using an existing
     * secret ID as the communication channel.
     *
     * @param secretId the id of the secret
     * @param version the version of the secret
     * @param helpers the helpers from whom to get the shares
     * @return a reconstructed secret
     */
    DeRecSecret recoverSecret(DeRecSecret.Id secretId, int version, List<? extends DeRecIdentity> helpers);

    /**
     * The application tells the library that the user has agreed that recovery is complete using this API.
     * This updates the state of the Sharer in the library to revert to normal mode, using their original keys and
     * communicating with their restored helpers in the contexts of their recovered secrets.
     *
     * @param recoverySecretId the id of the secret used for pairing during recovery mode
     */
    void recoveryComplete(DeRecSecret.Id recoverySecretId);

    /**
     * Provide a "listener" for status and lifecycle event notifications relating to this sharer's secrets.
     * <p>
     * Note: More than one listener may be provided by composition such as:
     * <p>
     * <pre>{@code
     * Consumer<DeRecStatusNotification> listener1 = n -> log(n.getType().name());
     * Consumer<DeRecStatusNotification> listener2 = n -> {if (n.getSeverity().equals(ERROR)) alert(n.getType().name());};
     * sharer.setListener(listener1.andThen(listener2));
     * }</pre>
     */
    void setListener(Consumer<DeRecStatusNotification> listener);
}
