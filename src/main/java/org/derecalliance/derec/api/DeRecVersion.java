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

/**
 * Represents an iteration of a value of a secret. A new Version is created when a {@link DeRecSecret} is updated.
 * It is distributed among helpers. If a sufficient number of helpers acknowledge receipt {@link #isProtected()}
 * returns true, representing that this version can be recovered.
 */
public interface DeRecVersion {

    /**
     * The secret this version is a secret of
     */
    DeRecSecret getSecret();

    /**
     * The version number of this Version. Later versions have higher numbers.
     */
    int getVersionNumber();

    /**
     * The value of the secret at this version
     */
    byte[] getProtectedValue();

    /**
     * The version has been successfully distributed among helpers.
     */
    boolean isProtected();

    /**
     * Get the list of helpers who are protecting this version.
     */
    List<DeRecHelperStatus> getProtectingHelperStatuses();
}
