package com.thebuildingblocks.derec.hse;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class Encryption {

    public static final String KEY_AGREEMENT_ALGO = "ECDH";
    public static final String KEY_PAIR_GENERATOR_ALGO = "EC";
    public static final String KEY_FACTORY_ALGO = "EC";
    private static final String ENCRYPT_ALGO = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    static final int IV_LENGTH_BYTE = 12;

    static SecureRandom secureRandom = new SecureRandom();
    static KeyFactory eckf;
    public static KeyPairGenerator kpg;



    static {
        try {
            kpg = KeyPairGenerator.getInstance(KEY_PAIR_GENERATOR_ALGO);
            eckf = java.security.KeyFactory.getInstance(KEY_FACTORY_ALGO);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        kpg.initialize(256);
    }

    /**
     * Deserialize a byte array as a PublicKey
     * @param key the byte array X.509 encoded
     * @return the Public Key
     */
    public static PublicKey publicKeyFromByteArray(byte[] key) {
        try {
            return eckf.generatePublic(new X509EncodedKeySpec(key));
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Make a ECDH Symmetric Key, whichever way round this is done i.e. whoever is "their" and "our",
     * it should end up with the same thing
     * @param ourPrivateKey our private key
     * @param theirPublicKey their public key
     * @return an ECDH Symmetric key
     */
    public static byte [] generateEcdhKey(PrivateKey ourPrivateKey, PublicKey theirPublicKey) {
        try {
            KeyAgreement keyAgreement = KeyAgreement.getInstance(KEY_AGREEMENT_ALGO);
            keyAgreement.init(ourPrivateKey);
            keyAgreement.doPhase(theirPublicKey, true);
            return keyAgreement.generateSecret();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Make a SecretKey for encryption (from an ecdh key)
     * @param ecdhKey the key
     * @return a SecretKey
     */
    public static SecretKey generateSecretKey(byte [] ecdhKey) {
        return new SecretKeySpec(ecdhKey, "AES");
    }

    /**
     * Get a 12 byte iv suitable for GCM
     * @return the iv
     */
    public static byte [] generateIv() {
        byte[] iv = new byte[IV_LENGTH_BYTE];
        secureRandom.nextBytes(iv);
        return iv;
    }

    /**
     * Encrypt using supplied secret key and iv
     * @param pText plainText to encrypt
     * @param secretKey the key to use
     * @param iv an iv
     * @return cipherText
     */
    public static byte[] doEncrypt(byte[] pText, SecretKey secretKey, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            return cipher.doFinal(pText);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get encrypted text prepended by iv
     * @param pText plain text
     * @param secret encryption secret
     * @param iv iv
     * @return a buffer
     */
    public static byte[] encryptWithPrefixIv(byte[] pText, SecretKey secret, byte[] iv) {

        byte[] cipherText = doEncrypt(pText, secret, iv);

        return ByteBuffer.allocate(iv.length + cipherText.length)
                .put(iv)
                .put(cipherText)
                .array();
    }

    /**
     * Decrypt using supplied ecdh key and iv
     * @param eText cipherText
     * @param secretKey secret key
     * @param iv iv
     * @return plain text
     */
    public static byte[] doDecrypt(byte[] eText, SecretKey secretKey, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            return cipher.doFinal(eText);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Decrypt with initial part of the cText being the iv
     * @param cText ciphertext
     * @param secret secret key
     * @return plain text
     */
    public static byte [] decryptWithPrefixIv(byte[] cText, SecretKey secret) {

        ByteBuffer bb = ByteBuffer.wrap(cText);

        byte[] iv = new byte[IV_LENGTH_BYTE];
        bb.get(iv);

        byte[] cipherText = new byte[bb.remaining()];
        bb.get(cipherText);

        return doDecrypt(cipherText, secret, iv);
    }
}
