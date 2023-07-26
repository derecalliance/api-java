package com.thebuildingblocks.derec.util;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import static com.thebuildingblocks.derec.util.Crypto.createCertificate;
import static com.thebuildingblocks.derec.util.Crypto.generateKeyPair;


/*
https://stackoverflow.com/questions/43960761/how-to-store-and-reuse-keypair-in-java/43965528#43965528
 */
public class Keystore {
    public static final String SIGNATURE_ALGORITHM = "SHA256WithRSA";
    public static final String KEY_PAIR_ALGORITHM = "RSA";
    public static final int KEY_SIZE = 2048;

    static Provider bcProvider = new BouncyCastleProvider();

    static {
        Security.addProvider(bcProvider);
    }


    public static void main(String[] args) throws Exception {
        KeyPair generatedKeyPair = generateKeyPair(KEY_PAIR_ALGORITHM, KEY_SIZE);

        String filename = "test_gen_self_signed.pkcs12";
        char[] password = "test".toCharArray();

        storeToPKCS12(filename, password, generatedKeyPair);

        KeyPair retrievedKeyPair = loadFromPKCS12(filename, password);

        // you can validate by generating a signature and verifying it or by
        // comparing the moduli by first casting to RSAPublicKey, e.g.:

        RSAPublicKey pubKey = (RSAPublicKey) generatedKeyPair.getPublic();
        RSAPrivateKey privKey = (RSAPrivateKey) retrievedKeyPair.getPrivate();
        System.out.println(pubKey.getModulus().equals(privKey.getModulus()));
    }

    private static KeyPair loadFromPKCS12(String filename, char[] password) throws KeyStoreException,
            NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableEntryException {
        KeyStore pkcs12KeyStore = KeyStore.getInstance("PKCS12");

        try (FileInputStream fis = new FileInputStream(filename)) {
            pkcs12KeyStore.load(fis, password);
        }

        KeyStore.ProtectionParameter param = new KeyStore.PasswordProtection(password);
        KeyStore.Entry entry = pkcs12KeyStore.getEntry("owlstead", param);
        if (!(entry instanceof KeyStore.PrivateKeyEntry privKeyEntry)) {
            throw new KeyStoreException("That's not a private key!");
        }
        PublicKey publicKey = privKeyEntry.getCertificate().getPublicKey();
        PrivateKey privateKey = privKeyEntry.getPrivateKey();
        return new KeyPair(publicKey, privateKey);
    }

    private static void storeToPKCS12(String filename, char[] password, KeyPair generatedKeyPair) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, OperatorCreationException {

        Certificate selfSignedCertificate = createCertificate(generatedKeyPair, "CN=owlstead", SIGNATURE_ALGORITHM);

        KeyStore pkcs12KeyStore = KeyStore.getInstance("PKCS12");
        pkcs12KeyStore.load(null, null);

        KeyStore.Entry entry = new KeyStore.PrivateKeyEntry(generatedKeyPair.getPrivate(),
                new Certificate[]{selfSignedCertificate});
        KeyStore.ProtectionParameter param = new KeyStore.PasswordProtection(password);

        pkcs12KeyStore.setEntry("owlstead", entry, param);

        try (FileOutputStream fos = new FileOutputStream(filename)) {
            pkcs12KeyStore.store(fos, password);
        }
    }
}
