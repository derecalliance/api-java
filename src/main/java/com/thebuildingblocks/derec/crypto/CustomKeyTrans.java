package com.thebuildingblocks.derec.crypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Objects;

/**
 * Sign then Encrypt Prototype.
 * <p>
 * Illustration of a proposed method for DeRec protocol using "Key Transfer" methodology, and custom encoding
 * <p>
 * Alice creates a buffer containing
 * <ol>
 *     <li>Her public key (necessary in initial key exchange in some scenarios)</li>
 *     <li>An encryption key (EK) encrypted using Bob's public key (which she must have got during key exchange)</li>
 *     <li>An IV </li>
 *     <li>Encrypted using EK: </li>
 *     <ol>
 *         <li>The message data</li>
 *         <li>A signature for the message data</li>
 *     </ol>
 * </ol>
 * <p>
 * Bob inverts the process and prints out the message Alice sent.
 */
public class CustomKeyTrans {

    public static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    public static final int SIGNATURE_LENGTH_BYTES = 256;
    public static final int ENCRYPTED_SECRET_KEY_LENGTH_BYTES = 256;
    public static final String KEY_CIPHER_ALGORITHM = "RSA/ECB/PKCS1Padding";
    public static final String KEY_FACTORY_ALGORITHM = "RSA";
    public static final String KEY_PAIR_ALGORITHM = "RSA";
    public static final int KEY_PAIR_KEY_SIZE = 2048;

    public static final String MESSAGE_CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    public static final String SECRET_KEY_ALGORITHM = "AES";
    public static final int SECRET_KEY_SIZE = 128;
    public static final int IV_LENGTH = 128 / 8;

    static Logger logger = LoggerFactory.getLogger(CustomKeyTrans.class.getSimpleName());
    static KeyPairGenerator kpg;
    static {
        try {
            kpg = KeyPairGenerator.getInstance(KEY_PAIR_ALGORITHM);
            kpg.initialize(KEY_PAIR_KEY_SIZE);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    static KeyPair aliceKeyPair = kpg.generateKeyPair();
    static KeyPair bobKeyPair = kpg.generateKeyPair();

    public static void main(String[] args) throws GeneralSecurityException, IOException {
        // create message
        byte[] protocolMessage = beAlice();
        // decode message
        String decoded = new String(beBob(protocolMessage), StandardCharsets.UTF_8);
    }

    public static byte[] beAlice() throws IOException, GeneralSecurityException {
        // alice is going to send Bob a poem
        try (InputStream is = CustomKeyTrans.class.getClassLoader().getResourceAsStream("jabberwocky.txt")) {
            byte[] plainText = Objects.requireNonNull(is).readAllBytes();

            try (ByteArrayOutputStream os = new ByteArrayOutputStream();
                 DataOutputStream dataOutputStream = new DataOutputStream(os)) {
                // adds her public key
                dataOutputStream.writeInt(aliceKeyPair.getPublic().getEncoded().length);
                dataOutputStream.write(aliceKeyPair.getPublic().getEncoded());
                // adds signed encrypted poem
                signThenEncrypt(aliceKeyPair.getPrivate(), bobKeyPair.getPublic(), plainText, dataOutputStream);

                // returns as a buffer
                byte[] byteArray = os.toByteArray();
                logger.info("Content was {} bytes, encrypted signed content {} bytes",
                        plainText.length, byteArray.length );
                return byteArray;
            }
        }
    }

    public static byte[] beBob(byte[] protocolMessage) throws IOException, GeneralSecurityException {
        // bob gets a message
        try (ByteArrayInputStream bais = new ByteArrayInputStream(protocolMessage);
             DataInputStream dataInputStream = new DataInputStream(bais)) {

            // message starts with Alice's public key
            byte[] alicePublicKeyBytes = new byte[dataInputStream.readInt()];
            dataInputStream.readFully(alicePublicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_FACTORY_ALGORITHM);
            PublicKey alicePublicKey = keyFactory.generatePublic(new X509EncodedKeySpec(alicePublicKeyBytes));

            // secret key encrypted with Bob's public key follows
            byte[] secretKeyBytes = new byte[ENCRYPTED_SECRET_KEY_LENGTH_BYTES];
            dataInputStream.readFully(secretKeyBytes);
            Cipher keyCipherInstance = Cipher.getInstance(KEY_CIPHER_ALGORITHM);
            keyCipherInstance.init(Cipher.DECRYPT_MODE, bobKeyPair.getPrivate());
            byte[] secretKeyEncryptedBytes = keyCipherInstance.doFinal(secretKeyBytes);
            SecretKeySpec secretKey = new SecretKeySpec(secretKeyEncryptedBytes, SECRET_KEY_ALGORITHM);

            // iv follows
            byte[] iv = new byte[IV_LENGTH];
            dataInputStream.readFully(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // rest of the message
            byte[] encryptedSignedBytes = dataInputStream.readAllBytes();

            return decryptThenVerify(alicePublicKey, secretKey, ivSpec, encryptedSignedBytes);
        }
    }


    /**
     * Sign then encrypt a message and its signature and serialize to an OutputStream
     *
     * @param myPrivateKey   the private key of the sender
     * @param theirPublicKey the public key of the recipient
     * @param message        the plain text message
     * @param outputStream   an output stream to write to
     * @throws IOException              if bad IO things happen
     * @throws GeneralSecurityException if bad security things happen
     */
    public static void signThenEncrypt(PrivateKey myPrivateKey, PublicKey theirPublicKey, byte[] message,
                                       OutputStream outputStream) throws IOException, GeneralSecurityException {
        // generate secret key with which to encrypt message
        KeyGenerator kgen = KeyGenerator.getInstance(SECRET_KEY_ALGORITHM);
        kgen.init(SECRET_KEY_SIZE);
        SecretKey secretKey = kgen.generateKey();

        // encrypt the secret key with their public key
        Cipher keyCipherInstance = Cipher.getInstance(KEY_CIPHER_ALGORITHM);
        keyCipherInstance.init(Cipher.ENCRYPT_MODE, theirPublicKey);
        byte[] b = keyCipherInstance.doFinal(secretKey.getEncoded());
        outputStream.write(b);

        // get an iv and write it
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        outputStream.write(ivSpec.getIV());

        // output encrypted message and signature
        Cipher messageCipherInstance = Cipher.getInstance(MESSAGE_CIPHER_ALGORITHM);
        messageCipherInstance.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
        outputStream.write(messageCipherInstance.update(message));
        outputStream.write(messageCipherInstance.update(createSignature(myPrivateKey, message)));
        outputStream.write(messageCipherInstance.doFinal());
    }

    /**
     * Take some cipher text and return a plain text buffer. Throw an exception if the signature is not correct
     *
     * @param publicKey  the public key of the sender
     * @param secretKey  the secret key they used to encrypt the message
     * @param ivSpec     the IV they sent with the message
     * @param cipherText the cipher text
     * @return a plain text byte array
     * @throws GeneralSecurityException if nasty security things
     * @throws IOException              if nasty IO things
     */
    public static byte[] decryptThenVerify(PublicKey publicKey, SecretKey secretKey, IvParameterSpec ivSpec,
                                           byte[] cipherText) throws GeneralSecurityException, IOException {
        // writing plaintext to a stream
        ByteArrayOutputStream payload = new ByteArrayOutputStream();

        // decrypt the cipherText
        Cipher messageCipherInstance = Cipher.getInstance(MESSAGE_CIPHER_ALGORITHM);
        messageCipherInstance.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
        payload.write(messageCipherInstance.doFinal(cipherText));

        // separate off the data and the signature bytes
        byte[] decryptedSignedBytes = payload.toByteArray();
        int dataLength = decryptedSignedBytes.length - SIGNATURE_LENGTH_BYTES;
        byte[] data = Arrays.copyOf(decryptedSignedBytes, dataLength);
        byte[] signature = Arrays.copyOfRange(decryptedSignedBytes, dataLength, dataLength + SIGNATURE_LENGTH_BYTES);

        verifySignature(publicKey, data, signature);

        return data;
    }

    public static byte[] createSignature(PrivateKey privateKey, byte[] message) throws GeneralSecurityException {
        Signature signatureInstance = Signature.getInstance(SIGNATURE_ALGORITHM);
        signatureInstance.initSign(privateKey);
        signatureInstance.update(message);
        return signatureInstance.sign();
    }

    public static void verifySignature(PublicKey publicKey, byte[] data, byte[] signature) throws GeneralSecurityException {
        Signature signatureInstance = Signature.getInstance(SIGNATURE_ALGORITHM);
        signatureInstance.initVerify(publicKey);
        signatureInstance.update(data);
        if (!signatureInstance.verify(signature)) {
            throw new RuntimeException("Signature does not match");
        }
    }
}
