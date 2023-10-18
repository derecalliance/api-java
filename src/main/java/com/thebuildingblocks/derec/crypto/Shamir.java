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

package com.thebuildingblocks.derec.crypto;

import com.codahale.shamir.Scheme;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Crude test of timing of splitting and joining Shamir secrets
 */
public class Shamir {
    /**
     * Result of a test contains:
     * @param secretSize size of the secret
     * @param shares number of shares
     * @param threshold number required
     * @param splitMillis time taken to split
     * @param joinMillis time taken to recombine
     */
    public record Result (int secretSize, int shares, int threshold, long splitMillis, long joinMillis){ }
    public static int oneK = 1024;
    public static byte [] signature = "JoRabin!".getBytes(StandardCharsets.UTF_8);

    public static void main(String [] args) {
        for (int size = 1; size <= 16; size ++) {
            for (int shares = 8; shares < 255; shares = shares +16) {
                System.out.println(measure(oneK * size, shares, shares/2));
            }
        }
    }

    private static Result measure(int secretSize, int shares, int threshold) {
        // create the scheme
        final Scheme scheme = new Scheme(new SecureRandom(), shares, threshold);

        // set up the secret to contain the signature followed by
        // bytes containing their index in the array
        final byte[] secret = new byte [secretSize];
        System.arraycopy(signature,0,secret,0, signature.length);
        for (int index = signature.length; index < secretSize; index++) {
            secret[index] = (byte) (index % 256);
        }

        // split the secret
        long splitStartTime = System.currentTimeMillis();
        final Map<Integer, byte[]> split = scheme.split(secret);
        long splitEndTime = System.currentTimeMillis();

        // create a map of the split with threshold entries
        final Map<Integer, byte[]> parts = split.entrySet()
                .stream()
                .limit(threshold)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // join the parts
        long joinStartTime = System.currentTimeMillis();
        byte [] secret2 = scheme.join(parts);
        long joinEndTime = System.currentTimeMillis();

        // verify that we got the signature back
        if (!Arrays.equals(signature,Arrays.copyOf(secret2, 8))) {
            throw new AssertionError("Joined secret did not contain signature");
        }
        return new Result(secretSize, shares, threshold, splitEndTime-splitStartTime, joinEndTime-joinStartTime);
    }
}