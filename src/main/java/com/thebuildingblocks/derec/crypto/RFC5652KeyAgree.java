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

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyAgreeEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JceKeyAgreeRecipientId;
import org.bouncycastle.cms.jcajce.JceKeyAgreeRecipientInfoGenerator;
import org.bouncycastle.operator.OutputEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Objects;

import static com.thebuildingblocks.derec.crypto.util.Crypto.createCertificate;
import static com.thebuildingblocks.derec.crypto.util.Crypto.generateKeyPair;

/**
 * Illustration of a proposed method for DeRec protocol using "Key Agreement" methodology, and RFC5652
 * Cryptographic Message Syntax (CMS)
 * <p>
 * Since this is a key agreement technique there is no need to sign the content
 */
public class RFC5652KeyAgree {

    public static final String SIGNATURE_ALGORITHM = "SHA1withECDSA";
    public static final String KEY_PAIR_ALGORITHM = "EC";
    public static final int KEY_SIZE = 256;
    public static final ASN1ObjectIdentifier KEY_AGREEMENT_ALGO = CMSAlgorithm.ECCDH_SHA384KDF;
    public static final ASN1ObjectIdentifier KEY_ENCRYPTION_ALGO = CMSAlgorithm.AES256_WRAP;
    static Logger logger = LoggerFactory.getLogger(RFC5652KeyAgree.class.getSimpleName());

    public static void main(String[] args) throws Exception {
        KeyPair aliceKeyPair = generateKeyPair(KEY_PAIR_ALGORITHM, KEY_SIZE);
        KeyPair bobKeyPair = generateKeyPair(KEY_PAIR_ALGORITHM, KEY_SIZE);

        X509Certificate aliceCertificate = createCertificate(aliceKeyPair, "CN=Alice", SIGNATURE_ALGORITHM);
        X509Certificate bobCertificate = createCertificate(bobKeyPair, "CN=Bob", SIGNATURE_ALGORITHM);

        try (InputStream is = RFC5652KeyAgree.class.getClassLoader().getResourceAsStream("jabberwocky.txt")) {
            byte[] plainText = Objects.requireNonNull(is).readAllBytes();

            // encrypt message
            byte[] message = createKeyAgreeEnvelopedObject(
                    aliceKeyPair.getPrivate(),
                    aliceCertificate,
                    bobCertificate,
                    plainText);

            // process encrypted message
            byte[] decryptedMessage = extractKeyAgreeEnvelopedData(bobKeyPair.getPrivate(), bobCertificate, message);
            if (!Arrays.equals(plainText, decryptedMessage)) {
                throw new AssertionError("Decrypted text is not the same as encrypted");
            }
            logger.info("Decrypted message is: \n{}", new String(decryptedMessage, StandardCharsets.UTF_8));
            logger.info("Plain text is {}, encrypted is {} bytes", plainText.length, message.length);
            logger.info("Decryption successful");
        }
    }

    /**
     * Create a CMS formatted buffer from plain text using key agreement
     * @param initiatorPrivateKey creator's private key
     * @param initiatorCert creator's certificate
     * @param recipientCert recipient's certificate
     * @param data the data to transfer
     * @return a buffer
     */
    public static byte[] createKeyAgreeEnvelopedObject(PrivateKey initiatorPrivateKey,
                                                       X509Certificate initiatorCert,
                                                       X509Certificate recipientCert,
                                                       byte[] data) throws GeneralSecurityException, CMSException, IOException {
        CMSEnvelopedDataGenerator envelopedGen = new CMSEnvelopedDataGenerator();

        JceKeyAgreeRecipientInfoGenerator recipientInfoGenerator =
                new JceKeyAgreeRecipientInfoGenerator(KEY_AGREEMENT_ALGO, initiatorPrivateKey, initiatorCert.getPublicKey(), KEY_ENCRYPTION_ALGO)
                .addRecipient(recipientCert)
                .setProvider("BC");
        envelopedGen.addRecipientInfoGenerator(recipientInfoGenerator);

        OutputEncryptor outputEncryptor = new JceCMSContentEncryptorBuilder(CMSAlgorithm.AES256_CBC)
                .setProvider("BC")
                .build();
        return envelopedGen.generate(new CMSProcessableByteArray(data), outputEncryptor).getEncoded();
    }

    /**
     * Take a CMS formatted buffer and return the plain text from it
     * @param recipientPrivateKey the processor's private key
     * @param recipientCert the processor's certificate
     * @param data the data to decrypt
     * @return plain text
     */
    public static byte[] extractKeyAgreeEnvelopedData(PrivateKey recipientPrivateKey,
                                                      X509Certificate recipientCert,
                                                      byte[] data) throws CMSException {
        CMSEnvelopedData envelopedData = new CMSEnvelopedData(data);
        RecipientInformationStore recipients = envelopedData.getRecipientInfos();
        RecipientId rid = new JceKeyAgreeRecipientId(recipientCert);
        RecipientInformation recipient = recipients.get(rid);
        return recipient.getContent(new JceKeyAgreeEnvelopedRecipient(recipientPrivateKey).setProvider("BC"));
    }
}
