package com.thebuildingblocks.shamir;

import com.codahale.shamir.Scheme;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Crude test of timing of splitting and joining Shamir secrets
 */
public class Main {
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