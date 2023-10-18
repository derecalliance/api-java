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

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
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
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

import static com.thebuildingblocks.derec.crypto.util.Crypto.createCertificate;
import static com.thebuildingblocks.derec.crypto.util.Crypto.generateKeyPair;

/**
 * Illustration of a proposed method for DeRec protocol using "Key Transfer" methodology, and RFC5652
 * Cryptographic Message Syntax (CMS)
 * <p>
 * The enveloped content is a Signature containing the plain text for transfer. i.e. this is a Sign then Encrypt
 * implementation.
 */
public class RFC5652KeyTrans {


    public static final String SIGNATURE_ALGORITHM = "SHA256WithRSA";
    public static final String KEY_PAIR_ALGORITHM = "RSA";
    public static final int KEY_SIZE = 2048;
    public static final String CONTENT_SIGNATURE_ALGORITHM = "SHA256withRSA";
    public static final ASN1ObjectIdentifier CONTENT_ENCRYPTION_ALGORITHM = CMSAlgorithm.AES128_GCM;
    static Logger logger = LoggerFactory.getLogger(RFC5652KeyTrans.class.getSimpleName());

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void main(String[] args) throws Exception {
        KeyPair aliceKeyPair = generateKeyPair(KEY_PAIR_ALGORITHM, KEY_SIZE);
        KeyPair bobKeyPair = generateKeyPair(KEY_PAIR_ALGORITHM, KEY_SIZE);

        X509Certificate aliceCertificate = createCertificate(aliceKeyPair, "CN=Alice", SIGNATURE_ALGORITHM);
        X509Certificate bobCertificate = createCertificate(bobKeyPair, "CN=Bob", SIGNATURE_ALGORITHM);

        try (InputStream is = RFC5652KeyTrans.class.getClassLoader().getResourceAsStream("jabberwocky.txt")) {
            byte[] plainText = Objects.requireNonNull(is).readAllBytes();

            // sign and encrypt message
            CMSSignedData signed = signData(plainText, aliceCertificate, aliceKeyPair.getPrivate());
            byte[] encryptedSigned = encryptData(signed, bobCertificate);

            // process encrypted message
            byte[] decryptedSigned = decryptData(encryptedSigned, bobKeyPair.getPrivate());
            CMSSignedData signedData = buildSignedData(decryptedSigned);
            if (!verifySignedData(signedData)) {
                logger.error("Signature error");
                return;
            }
            byte []decryptedMessage = getData(signedData);
            if (!Arrays.equals(plainText, decryptedMessage)) {
                throw new AssertionError("Decrypted text is not the same as encrypted");
            }
            logger.info("Incoming message from {}",
                    signedData.getSignerInfos().getSigners().iterator().next().getSID().getIssuer());
            logger.info("Decrypted Signed message: {}\n", new String(decryptedMessage, StandardCharsets.UTF_8));

            logger.info("Content was {} bytes, signed content {} bytes and encrypted signed content {} bytes",
                    plainText.length, signed.getEncoded().length, encryptedSigned.length);
        }
    }

    /**
     * Sign some data
     *
     * @param data               the data to sign
     * @param signingCertificate the certificate to use for signing
     * @param signingKey         the private key to use for signing
     * @return a CMSSignedData object
     */
    public static CMSSignedData signData(byte[] data,
                                         final X509Certificate signingCertificate,
                                         final PrivateKey signingKey)
            throws CertificateEncodingException, OperatorCreationException, CMSException {

        JcaCertStore certStore = new JcaCertStore(List.of(signingCertificate));

        ContentSigner contentSigner = new JcaContentSignerBuilder(CONTENT_SIGNATURE_ALGORITHM)
                .build(signingKey);
        DigestCalculatorProvider digestCalculatorProvider =
                new JcaDigestCalculatorProviderBuilder()
                        .setProvider("BC")
                        .build();
        SignerInfoGenerator signerInfoGenerator =
                new JcaSignerInfoGeneratorBuilder(digestCalculatorProvider)
                        .build(contentSigner, signingCertificate);
        CMSSignedDataGenerator cmsGenerator = new CMSSignedDataGenerator();
        cmsGenerator.addSignerInfoGenerator(signerInfoGenerator);
        cmsGenerator.addCertificates(certStore);

        CMSTypedData cmsData = new CMSProcessableByteArray(data);
        return cmsGenerator.generate(cmsData, true);
    }

    /**
     * build a SignedData instance from an encoded buffer
     *
     * @param encodedData ASN1 encoded input data
     * @return a CMSSignedData object
     */
    public static CMSSignedData buildSignedData(final byte[] encodedData) throws CMSException, IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(encodedData);
             ASN1InputStream asn1InputStream = new ASN1InputStream(bais)) {
            return buildSignedData(asn1InputStream);
        }
    }

    /**
     * build a SignedData instance from an encoded input stream
     *
     * @param inputStream ASN1 encoded input stream - caller to close
     * @return a CMSSignedData object
     */
    public static CMSSignedData buildSignedData(final InputStream inputStream) throws CMSException, IOException {
        ASN1InputStream asn1InputStream = new ASN1InputStream(inputStream);
        return new CMSSignedData(ContentInfo.getInstance(asn1InputStream.readObject()));
    }

    /**
     * Get the data that this signature signed (assuming that it contains that data).
     *
     * @param data a CMSSignedData object
     * @return the content
     */
    public static byte[] getData(CMSSignedData data) {
        return (byte[]) data.getSignedContent().getContent();
    }

    /**
     * Verify that the signed data was correctly signed by the purported signer.
     * <p>
     * Assumes only one signature.
     *
     * @param cmsSignedData a CMSSignedData object
     * @return true if the verification is successful
     */
    public static boolean verifySignedData(CMSSignedData cmsSignedData) throws CertificateException,
            OperatorCreationException, CMSException {
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

    public static byte[] encryptData(CMSSignedData data, X509Certificate encryptionCertificate)
            throws CertificateEncodingException, CMSException, IOException {
        return encryptData(data.getEncoded(), encryptionCertificate);
    }

    public static byte[] encryptData(final byte[] data, X509Certificate encryptionCertificate)
            throws CertificateEncodingException, CMSException, IOException {
        CMSTypedData msg = new CMSProcessableByteArray(data);
        return encryptData(msg, encryptionCertificate);
    }

    public static byte[] encryptData(CMSTypedData msg, X509Certificate encryptionCertificate)
            throws CertificateEncodingException, CMSException, IOException {
        CMSEnvelopedDataGenerator cmsEnvelopedDataGenerator = new CMSEnvelopedDataGenerator();
        JceKeyTransRecipientInfoGenerator jceKey = new JceKeyTransRecipientInfoGenerator(encryptionCertificate);
        cmsEnvelopedDataGenerator.addRecipientInfoGenerator(jceKey);
        OutputEncryptor encryptor =
                new JceCMSContentEncryptorBuilder(CONTENT_ENCRYPTION_ALGORITHM).setProvider("BC").build();
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