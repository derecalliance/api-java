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

import java.util.List;

/**
 * A factory for and container of Secrets in this API
 */
public interface DeRecSharer {

    /**
     * Create a new secret and auto-allocate its ID
     *
     * @param description    a human readable description
     * @param bytesToProtect the content of the secret
     * @param helperIds      the ids of helpers for this secret
     * @return a secret
     */
    DeRecSecret newSecret(String description, byte[] bytesToProtect, List<DeRecId> helperIds);

    /**
     * Create a new secret.
     *
     * @param secretId       1 to 16 bytes that uniquely identify this secret for this sharer
     * @param description    a human readable description
     * @param bytesToProtect the content of the secret
     * @param helperIds      the ids of helpers for this secret
     * @return a secret
     */
    DeRecSecret newSecret(byte[] secretId, String description, byte[] bytesToProtect, List<DeRecId> helperIds);

    /**
     * Get the secret with this UUID, return null if none with this ID
     *
     * @param secretId a secret ID
     * @return a secret or null
     */
    DeRecSecret getSecret(byte[] secretId);

    /**
     * Get a list of all secrets known to this sharer
     *
     * @return a list
     */
    List<? extends DeRecSecret> getSecrets();
}
