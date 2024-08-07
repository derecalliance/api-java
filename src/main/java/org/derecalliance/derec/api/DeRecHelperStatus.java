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

import java.time.Instant;

/**
 * Representation of a helper as perceived by a sharer
 */
public interface DeRecHelperStatus extends DeRecPairingStatus {
    DeRecIdentity getId();

    /**
     * Gets the last time at which the Helper had last successfully responded to a VerifyShareRequestMessage
     *
     * @return Instant time
     */
    Instant getLastVerificationTime();
}
