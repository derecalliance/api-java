package com.thebuildingblocks.derec.hse;

import javax.crypto.SecretKey;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.text.Normalizer;
import java.util.Arrays;

import static com.thebuildingblocks.derec.hse.Encryption.*;

public class Message {


    static short PROTOCOL_VERSION = 1;
    static short PAIRING_REQUEST_ID = 0;
    static short PAIRING_RESPONSE_ID = 1;

    public static class PairingRequest extends Message {
        public final String originatorName;
        final PublicKey originatorPublicKey;

        PairingRequest(String originatorName, PublicKey originatorPublicKey) {
            this.originatorName = originatorName;
            this.originatorPublicKey = originatorPublicKey;
        }

        public static PairingRequest deserialize(byte[] message) {
            ByteBuffer bb = ByteBuffer.wrap(message);
            short protocolVersion = bb.getShort();
            assert (protocolVersion == 1);
            short messageId = bb.getShort();
            assert (messageId == PAIRING_REQUEST_ID);

            short nameLength = bb.getShort();
            byte[] nameBuf = new byte[nameLength];
            bb.get(nameBuf);
            String originatorName = new String(nameBuf, StandardCharsets.UTF_8);

            int publicKeyLength = bb.getInt();
            byte[] originatorPublicKey = new byte[publicKeyLength];
            bb.get(originatorPublicKey);
            return new PairingRequest(originatorName, publicKeyFromByteArray(originatorPublicKey));
        }

        /* this is not encrypted */
        public byte[] serialize() {
            // todo optimize buffer
            byte[] buffer = new byte[2048];
            ByteBuffer bb = ByteBuffer.wrap(buffer)
                    .putShort(PROTOCOL_VERSION)
                    .putShort(PAIRING_REQUEST_ID)
                    .putShort((short) originatorName.length())
                    .put(originatorName.getBytes(StandardCharsets.UTF_8))
                    .putInt(originatorPublicKey.getEncoded().length)
                    .put(originatorPublicKey.getEncoded());
            return Arrays.copyOf(bb.array(), bb.position());
        }
    }

    public static class PairingResponse extends Message {
        // encrypted payload
        public final String originatorName;
        final String destinationName;

        // other fields

        PairingResponse(String originatorName, String destinationName) {
            this.originatorName = Normalizer.normalize(originatorName, Normalizer.Form.NFC);
            this.destinationName = Normalizer.normalize(destinationName, Normalizer.Form.NFC);
        }

        public static PairingResponse deserialize(byte[] message, PrivateKey myPrivateKey) {
            ByteBuffer bb = ByteBuffer.wrap(message);
            short protocolVersion = bb.getShort();
            assert (protocolVersion == 1);
            short messageId = bb.getShort();
            assert (messageId == PAIRING_RESPONSE_ID);

            // get their public key
            int pkl = bb.getInt();
            byte[] theirPublicKey = new byte[pkl];
            bb.get(theirPublicKey);
            // make the ecdh key
            byte[] ecdhKey = generateEcdhSecret(myPrivateKey, publicKeyFromByteArray(theirPublicKey));
            // get the secret key
            SecretKey secretKey = generateSecretKey(ecdhKey);

            // get the iv
            byte[] iv = new byte[IV_LENGTH_BYTE];
            bb.get(iv);

            byte[] cipherText = Arrays.copyOfRange(bb.array(), bb.position(), bb.capacity());

            byte[] plainText = doDecrypt(cipherText, secretKey, iv);

            // decode the unencrypted payload into fields
            ByteBuffer payload = ByteBuffer.wrap(plainText);
            short nameLength = payload.getShort();
            byte[] nameBuf = new byte[nameLength];
            payload.get(nameBuf);
            // we don't know what the destination is (it is the deserialiser, but we don't care anyway)
            return new PairingResponse(new String(nameBuf, StandardCharsets.UTF_8), "");
        }

        byte[] encrypt(SecretKey secretKey, byte[] iv) {
            ByteBuffer bb = ByteBuffer.allocate(1024) // todo random length
                    .putShort((short) originatorName.getBytes(StandardCharsets.UTF_8).length)
                    .put(originatorName.getBytes(StandardCharsets.UTF_8));
            byte[] plainText = Arrays.copyOf(bb.array(), bb.position());
            return doEncrypt(plainText, secretKey, iv);
        }

        public byte[] serialize(SecretKey secretKey, PublicKey publicKey) {
            byte[] iv = generateIv();
            byte[] cipherText = encrypt(secretKey, iv);
            ByteBuffer bb = ByteBuffer.allocate(cipherText.length + publicKey.getEncoded().length + iv.length + 8)
                    .putShort(PROTOCOL_VERSION)
                    .putShort(PAIRING_RESPONSE_ID)
                    .putInt(publicKey.getEncoded().length)
                    .put(publicKey.getEncoded())
                    .put(iv)
                    .put(cipherText);
            return Arrays.copyOf(bb.array(), bb.position());
        }
    }

    public record PairingAck(byte[] message) {

    }
}
