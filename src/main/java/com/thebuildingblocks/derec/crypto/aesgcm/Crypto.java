package com.thebuildingblocks.derec.crypto.aesgcm;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;
import java.util.Objects;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import static java.util.Map.entry;

/**
 * Contains static members to support key pair management and encryption
 */
public class Crypto {

    /**
     * Matching algorithms for Keys
     */
    public record KeyParams(String keyAgreementAlgo, String KeyPairGeneratorAlgo, String keyFactoryAlgo){}

    /**
     * Predefined sets of matching algos
     * <p>
     * See <a href="https://docs.oracle.com/en/java/javase/17/docs/specs/security/standard-names.html">Java Security Standard Algorithm Names</a>
     */
    public static Map<String, KeyParams> KEY_PARAMS_MAP = Map.ofEntries(
        entry("ECDH", new KeyParams("ECDH", "EC", "EC")),
        entry("XDH", new KeyParams("XDH", "X25519", "X25519")),
        // there doesn't seem to be a KeyAgreement Algo for Ed25519 in the standard JDK, so this doesn't work
        entry("Ed25519", new KeyParams("Ed25519", "Ed25519", "Ed25519"))
    );

    // choose ECDH by default
    public static KeyParams KEY_PARAMS = KEY_PARAMS_MAP.get("ECDH");
    public static final String ENCRYPT_ALGO = "AES/GCM/NoPadding";
    public static final int TAG_LENGTH_BIT = 128;
    public static final int IV_LENGTH_BYTE = 12;

    static SecureRandom secureRandom = new SecureRandom();
    static KeyFactory keyFactory;
    public static KeyPairGenerator keyPairGenerator;



    static {
        // use BC provider if requested
        if (!Objects.isNull(System.getProperty("BC")) && Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        try {
            keyPairGenerator = KeyPairGenerator.getInstance(KEY_PARAMS.KeyPairGeneratorAlgo);
            keyFactory = java.security.KeyFactory.getInstance(KEY_PARAMS.keyFactoryAlgo);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        //kpg.initialize(256);
    }

    /**
     * Deserialize a byte array as a PublicKey
     * @param key the byte array X.509 encoded
     * @return the Public Key
     */
    public static PublicKey publicKeyFromByteArray(byte[] key) {
        try {
            return keyFactory.generatePublic(new X509EncodedKeySpec(key));
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Make a shared secret, whichever way round this is done i.e. whoever is "their" and "our",
     * it should end up with the same thing
     * @param ourPrivateKey our private key
     * @param theirPublicKey their public key
     * @return a secret
     */
    public static byte [] generateSharedSecret(PrivateKey ourPrivateKey, PublicKey theirPublicKey) {
        try {
            KeyAgreement keyAgreement = KeyAgreement.getInstance(KEY_PARAMS.keyAgreementAlgo);
            keyAgreement.init(ourPrivateKey);
            keyAgreement.doPhase(theirPublicKey, true);
            return keyAgreement.generateSecret();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Make a SecretKey for encryption (from a [shared] secret)
     * <p>
     * TODO raw use of the shared key not recommended?
     * @param secret a secret
     * @return a SecretKey
     */
    public static SecretKey generateSecretKey(byte [] secret) {
        return new SecretKeySpec(secret, "AES");
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
     * Decrypt using supplied secret key and iv
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
