package com.thebuildingblocks.derec;

import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

import static com.thebuildingblocks.derec.hse.Encryption.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class AESGCMTest {

    KeyPair alice;
    KeyPair bob;

    @Before
    public void init() {
        alice = kpg.generateKeyPair();
        bob= kpg.generateKeyPair();
    }

    @Test
    public void testGenerateEcdhKey() {

        byte [] secret1 = generateEcdhKey(alice.getPrivate(), bob.getPublic());
        byte [] secret2 = generateEcdhKey(bob.getPrivate(), alice.getPublic());

        assertArrayEquals(secret1, secret2);
    }

    @Test
    public void testGenerateEcdhKeyFromByteArray() {
        PublicKey bobPublic = publicKeyFromByteArray(bob.getPublic().getEncoded());
        byte [] secret1 = generateEcdhKey(alice.getPrivate(), bobPublic);
        byte [] secret2 = generateEcdhKey(bob.getPrivate(), alice.getPublic());

        assertArrayEquals(secret1, secret2);
    }

    @Test
    public void testUnlimitedPolicy() throws NoSuchAlgorithmException {
        // check unlimited
        assertEquals(2147483647, javax.crypto.Cipher.getMaxAllowedKeyLength("AES"));
    }

    @Test
    public void testEncrypt() {
        byte [] plainText = "test 123".getBytes(StandardCharsets.UTF_8);
        byte [] iv = generateIv(); // iv is communicated "somehow"

        // Alice does encryption
        byte [] aliceEcdhKey = generateEcdhKey(alice.getPrivate(), bob.getPublic());
        byte [] cipherText = doEncrypt(plainText, generateSecretKey(aliceEcdhKey), iv);

        // Bob does decryption
        byte [] bobEcdhKey = generateEcdhKey(bob.getPrivate(), alice.getPublic());
        assertArrayEquals(plainText, doDecrypt(cipherText, generateSecretKey(bobEcdhKey), iv));
    }

    @Test
    public void testEncryptWithIv() {
        byte [] plainText = "test 123".getBytes(StandardCharsets.UTF_8);
        byte [] iv = generateIv();

        // Alice does encryption
        byte [] aliceEcdhKey = generateEcdhKey(alice.getPrivate(), bob.getPublic());
        byte [] cipherTextWithIv = encryptWithPrefixIv(plainText, generateSecretKey(aliceEcdhKey), iv);

        // Bob does decryption
        byte [] bobEcdhKey = generateEcdhKey(bob.getPrivate(), alice.getPublic());
        assertArrayEquals(plainText, decryptWithPrefixIv(cipherTextWithIv, generateSecretKey(bobEcdhKey)));
    }
}