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
 * The interface is for use by a higher level which extracts the relevant bits from a Version of a Secret
 * into a serial form and reconstructs the Version from the serialisations of its shares.
 */
public interface DeRecShareProvider {
    /**
     * Create shares from an array of bytes
     * @param bytesToShare the bytes to share
     * @param count the number of shares
     * @param threshold the recombination threshold
     * @return a list of count shares
     */
    List<byte[]> share (byte[] bytesToShare, int count, int threshold);

    /**
     * Recombine some shares
     * @param shares the shares to combine
     * @return the combined shares
     */
    byte [] combine (List<byte[]> shares);
}
