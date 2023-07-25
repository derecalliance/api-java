package com.thebuildingblocks.derec;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.ZonedDateTime;
import java.util.Date;


/*
https://stackoverflow.com/questions/43960761/how-to-store-and-reuse-keypair-in-java/43965528#43965528
 */
public class SelfSignedCertificate {


    static Provider bcProvider = new BouncyCastleProvider();
    static {
        Security.addProvider(bcProvider);
    }

    public static final String SIGNATURE_ALGORITHM = "SHA256WithRSA";
    public static final String KEY_PAIR_ALGORITHM = "RSA";
    public static final int KEY_SIZE = 2048;

    public static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(KEY_PAIR_ALGORITHM);
        generator.initialize(KEY_SIZE, new SecureRandom());
        return generator.generateKeyPair();
    }

    public static X509Certificate selfSign(KeyPair keyPair, String subjectDN) throws OperatorCreationException,
            CertificateException {

        ZonedDateTime start = ZonedDateTime.now();
        Date startDate = Date.from(start.toInstant());
        Date endDate = Date.from(start.plusYears(1).toInstant());

        X500Name dnName = new X500Name(subjectDN);

        // Using the current timestamp as the certificate serial number
        BigInteger certSerialNumber = new BigInteger(Long.toString(startDate.getTime()));

        SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());

        X509v3CertificateBuilder certificateBuilder = new X509v3CertificateBuilder(dnName, certSerialNumber,
                startDate, endDate, dnName, subjectPublicKeyInfo);

        ContentSigner contentSigner = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
                .setProvider(bcProvider)
                .build(keyPair.getPrivate());

        X509CertificateHolder certificateHolder = certificateBuilder.build(contentSigner);

        return new JcaX509CertificateConverter().getCertificate(certificateHolder);
    }

    public static void main(String[] args) throws Exception {
        KeyPair generatedKeyPair = generateKeyPair();

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
            NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException,
            UnrecoverableEntryException {
        KeyStore pkcs12KeyStore = KeyStore.getInstance("PKCS12");

        try (FileInputStream fis = new FileInputStream(filename);) {
            pkcs12KeyStore.load(fis, password);
        }

        KeyStore.ProtectionParameter param = new KeyStore.PasswordProtection(password);
        KeyStore.Entry entry = pkcs12KeyStore.getEntry("owlstead", param);
        if (!(entry instanceof KeyStore.PrivateKeyEntry)) {
            throw new KeyStoreException("That's not a private key!");
        }
        KeyStore.PrivateKeyEntry privKeyEntry = (KeyStore.PrivateKeyEntry) entry;
        PublicKey publicKey = privKeyEntry.getCertificate().getPublicKey();
        PrivateKey privateKey = privKeyEntry.getPrivateKey();
        return new KeyPair(publicKey, privateKey);
    }

    private static void storeToPKCS12(String filename, char[] password, KeyPair generatedKeyPair) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, FileNotFoundException, OperatorCreationException {

        Certificate selfSignedCertificate = selfSign(generatedKeyPair, "CN=owlstead");

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
