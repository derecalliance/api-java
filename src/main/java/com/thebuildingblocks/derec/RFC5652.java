package com.thebuildingblocks.derec;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.OutputEncryptor;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Selector;
import org.bouncycastle.util.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static com.thebuildingblocks.derec.SelfSignedCertificate.generateKeyPair;
import static com.thebuildingblocks.derec.SelfSignedCertificate.selfSign;

public class RFC5652 {

    public static final String CONTENT_SIGNATURE_ALGORITHM = "SHA256withRSA";
    static Logger logger = LoggerFactory.getLogger("CMS-PCKS#7");
    static {
        Security.addProvider(new BouncyCastleProvider());
    }
    public static void main (String[] args) throws Exception {
        KeyPair aliceKeyPair = generateKeyPair();
        KeyPair bobKeyPair = generateKeyPair();

        X509Certificate aliceCertificate = selfSign(aliceKeyPair, "CN=Alice");
        X509Certificate bobCertificate = selfSign(bobKeyPair, "CN=Bob");

        try (InputStream is = RFC5652.class.getClassLoader().getResourceAsStream("jabberwocky.txt")) {
            byte[] plainText = Objects.requireNonNull(is).readAllBytes();
            CMSSignedData signed = signData(plainText, aliceCertificate, aliceKeyPair.getPrivate());
            byte[] encryptedSigned = encryptData(signed, bobCertificate);

            byte[] decryptedSigned = decryptData(encryptedSigned, bobKeyPair.getPrivate());
            CMSSignedData signedData = buildSignedData(decryptedSigned);
            if (!verifySignedData(signedData)) {
                logger.error("Signature error");
            } else {
                logger.info("Incoming message from {}", signedData.getSignerInfos().getSigners().iterator().next().getSID().getIssuer());
                logger.info("Decrypted Signed message: {}", new String(getData(signedData), StandardCharsets.UTF_8));
            }
        }
    }

    public static CMSSignedData signData(byte[] data, final X509Certificate signingCertificate, final PrivateKey signingKey) throws CertificateEncodingException, OperatorCreationException, CMSException {

        JcaCertStore certStore = new JcaCertStore(List.of(signingCertificate));

        ContentSigner contentSigner = new JcaContentSignerBuilder(CONTENT_SIGNATURE_ALGORITHM).build(signingKey);

        DigestCalculatorProvider digestCalculatorProvider = new JcaDigestCalculatorProviderBuilder().setProvider("BC").build();
        SignerInfoGenerator signerInfoGenerator = new JcaSignerInfoGeneratorBuilder(digestCalculatorProvider).build(contentSigner, signingCertificate);
        CMSSignedDataGenerator cmsGenerator = new CMSSignedDataGenerator();
        cmsGenerator.addSignerInfoGenerator(signerInfoGenerator);
        cmsGenerator.addCertificates(certStore);

        CMSTypedData cmsData = new CMSProcessableByteArray(data);
        return cmsGenerator.generate(cmsData, true);
    }

    public static CMSSignedData buildSignedData(final byte[] data) throws CMSException, IOException {
        try (ByteArrayInputStream bIn = new ByteArrayInputStream(data);
             ASN1InputStream aIn = new ASN1InputStream(bIn)) {

            return new CMSSignedData(ContentInfo.getInstance(aIn.readObject()));
        }
    }
    public static byte[] getData(CMSSignedData data) {
        return (byte[]) data.getSignedContent().getContent();
    }

    public static boolean verifySignedData(CMSSignedData cmsSignedData) throws CertificateException, OperatorCreationException, CMSException {
        SignerInformationStore signers = cmsSignedData.getSignerInfos();
        Collection<SignerInformation> c = signers.getSigners();
        SignerInformation signer = c.iterator().next();
        //noinspection unchecked
        Selector<X509CertificateHolder> signerId = signer.getSID();
        Store<X509CertificateHolder> certs = cmsSignedData.getCertificates();
        Collection<X509CertificateHolder> certCollection = certs.getMatches(signerId);
        Iterator<X509CertificateHolder> certIt = certCollection.iterator();
        X509CertificateHolder certHolder = certIt.next();

        return signer.verify(new JcaSimpleSignerInfoVerifierBuilder().build(certHolder));
    }

    public static byte[] encryptData(CMSSignedData data, X509Certificate encryptionCertificate) throws CertificateEncodingException, CMSException, IOException {
        return encryptData(data.getEncoded(), encryptionCertificate);
    }

    public static byte[] encryptData(final byte[] data, X509Certificate encryptionCertificate) throws CertificateEncodingException, CMSException, IOException {
        CMSTypedData msg = new CMSProcessableByteArray(data);
        return encryptData(msg, encryptionCertificate);
    }

    public static byte[] encryptData(CMSTypedData msg, X509Certificate encryptionCertificate) throws CertificateEncodingException, CMSException, IOException {
            CMSEnvelopedDataGenerator cmsEnvelopedDataGenerator = new CMSEnvelopedDataGenerator();
            JceKeyTransRecipientInfoGenerator jceKey = new JceKeyTransRecipientInfoGenerator(encryptionCertificate);
            cmsEnvelopedDataGenerator.addRecipientInfoGenerator(jceKey);
            OutputEncryptor encryptor = new JceCMSContentEncryptorBuilder(CMSAlgorithm.AES128_CBC).setProvider("BC").build();
            CMSEnvelopedData cmsEnvelopedData = cmsEnvelopedDataGenerator.generate(msg, encryptor);
            return cmsEnvelopedData.getEncoded();
    }

    public static byte[] decryptData(final byte[] encryptedData, final PrivateKey decryptionKey) throws CMSException {
            CMSEnvelopedData envelopedData = new CMSEnvelopedData(encryptedData);
            Collection<RecipientInformation> recip = envelopedData.getRecipientInfos().getRecipients();
            KeyTransRecipientInformation recipientInfo = (KeyTransRecipientInformation) recip.iterator().next();
            JceKeyTransRecipient recipient = new JceKeyTransEnvelopedRecipient(decryptionKey);
            return recipientInfo.getContent(recipient);
    }
}