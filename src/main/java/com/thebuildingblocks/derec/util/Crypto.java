package com.thebuildingblocks.derec.util;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.util.Date;

public class Crypto {
    static Provider bcProvider = new BouncyCastleProvider();

    static {
        Security.addProvider(bcProvider);
    }

    public static KeyPair generateKeyPair(String keyPairAlgorithm, int keySize) throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(keyPairAlgorithm);
        keyPairGenerator.initialize(keySize);
        return keyPairGenerator.generateKeyPair();
    }

    public static X509Certificate createCertificate(KeyPair keyPair, String subjectDN, String signatureAlgorithm)
            throws OperatorCreationException, CertificateException, CertIOException {

        ZonedDateTime start = ZonedDateTime.now();
        Date startDate = Date.from(start.toInstant());
        Date endDate = Date.from(start.plusYears(1).toInstant());

        X500Name dnName = new X500Name(subjectDN);

        // Using the current timestamp as the certificate serial number
        BigInteger certSerialNumber = new BigInteger(Long.toString(startDate.getTime()));

        SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());

        X509v3CertificateBuilder certificateBuilder = new X509v3CertificateBuilder(dnName, certSerialNumber,
                startDate, endDate, dnName, subjectPublicKeyInfo);

        certificateBuilder.addExtension(Extension.keyUsage,true, new KeyUsage(KeyUsage.dataEncipherment
                | KeyUsage.keyEncipherment | KeyUsage.keyAgreement | KeyUsage.digitalSignature));

        ContentSigner contentSigner = new JcaContentSignerBuilder(signatureAlgorithm)
                .setProvider(bcProvider)
                .build(keyPair.getPrivate());

        X509CertificateHolder certificateHolder = certificateBuilder.build(contentSigner);

        return new JcaX509CertificateConverter().getCertificate(certificateHolder);
    }
}
