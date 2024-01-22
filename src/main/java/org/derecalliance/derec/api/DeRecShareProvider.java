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
 * Interface for sharing and recombining byte arrays. It's not the job of the provider to make
 * the shares useful for recovery, merely to split byte arrays up and recombine them.
 * <p>
 * The interface is for use by a higher level which adds metadata to the raw bytes of the secret
 * such as the list of helpers with which it was shared, private keys and so on.
 * <p>
 * The byte array shared with a helper (as provided by a call to {@link #share(byte[], int, byte[], int, int)})
 * will be shared in a {@code StoreShare} protobuf message. This will contain (unknown to the helper) a
 * {@code Share} protobuf message (if the sharer so chooses).
 * <p>
 * When using algorithm 0 (see protobufs), the {@code share} method wraps this in a {@code CommittedDeRecShare} message,
 * but this is not visible to the caller.
 */
public interface DeRecShareProvider {
    /**
     * Some kind of error was encountered when recombining shares
     */
    class RecombinationException extends Exception {}

    /**
     * Not enough shares were provided to recombine
     */
    class ThresholdException extends RecombinationException {}

    /**
     * Create shares from an array of bytes
     * @param secretId the id of the secret
     * @param version the version number of the secret
     * @param bytesToShare the bytes of the secret and any additional metadata
     * @param count the number of shares to create
     * @param threshold the recombination threshold
     * @return a list of [count] shares
     */
    List<byte[]> share (byte[] secretId, int version, byte[] bytesToShare, int count, int threshold);

    /**
     * Recombine some shares
     * @param shares the shares to combine
     * @return the combined shares
     */
    byte [] combine (List<byte[]> shares);
}
