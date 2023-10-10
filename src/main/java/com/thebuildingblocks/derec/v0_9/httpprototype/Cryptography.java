package com.thebuildingblocks.derec.v0_9.httpprototype;

import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Cryptography {

    public static MessageDigest messageDigest;
    public static KeyPairGenerator keyPairGenerator;

    static {
        try {
            messageDigest = MessageDigest.getInstance("SHA-384");
            keyPairGenerator = KeyPairGenerator.getInstance("EC");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
