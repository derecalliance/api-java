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

package com.thebuildingblocks.derec.crypto.aesgcm;

import javax.crypto.SecretKey;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.text.Normalizer;
import java.util.Arrays;

import static com.thebuildingblocks.derec.crypto.aesgcm.Crypto.*;

/**
 * Contains serializers and deserializers for protocol messages
 */
public class Message {

    static short PROTOCOL_VERSION = 1;
    static short PAIRING_REQUEST_ID = 0;
    static short PAIRING_RESPONSE_ID = 1;

    /**
     * Checks the message is at the right protocol version and is of the right kind
     * @param bb a byte buffer from which to take fields
     * @param messageIdRequired the message type expected
     */
    static void check(ByteBuffer bb, short messageIdRequired) {
        short protocolVersion = bb.getShort();
        short messageId = bb.getShort();
        if (protocolVersion != 1 || messageId != messageIdRequired) {
            throw new AssertionError("Wrong protocol version or message resultType");
        }
    }

    /**
     * Sent from User to Helper to initiate relationship
     * <p>
     * TODO add parameter negotiation
     */
    public static class PairingRequest extends Message {
        public final String originatorName;
        final PublicKey originatorPublicKey;

        PairingRequest(String originatorName, PublicKey originatorPublicKey) {
            this.originatorName = Normalizer.normalize(originatorName, Normalizer.Form.NFC);
            this.originatorPublicKey = originatorPublicKey;
        }

        public static PairingRequest deserialize(byte[] message) {
            ByteBuffer bb = ByteBuffer.wrap(message);
            check(bb, PAIRING_REQUEST_ID);

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

    /**
     * Send from Helper to User to acknowledge relationship (or TODO reject it)
     */
    public static class PairingResponse extends Message {
        // not transmitted
        final transient String destinationName;

        // encrypted payload
        public final String originatorName;

        // other fields

        PairingResponse(String originatorName, String destinationName) {
            this.originatorName = Normalizer.normalize(originatorName, Normalizer.Form.NFC);
            this.destinationName = Normalizer.normalize(destinationName, Normalizer.Form.NFC);
        }

        public static PairingResponse deserialize(byte[] message, PrivateKey myPrivateKey) {
            ByteBuffer bb = ByteBuffer.wrap(message);
            check(bb, PAIRING_RESPONSE_ID);

            // get their public key
            int pkl = bb.getInt();
            byte[] theirPublicKey = new byte[pkl];
            bb.get(theirPublicKey);
            // make the secret
            byte[] secret = generateSharedSecret(myPrivateKey, publicKeyFromByteArray(theirPublicKey));
            // get the secret key
            SecretKey secretKey = generateSecretKey(secret);

            // get the iv
            byte[] iv = new byte[IV_LENGTH_BYTE];
            bb.get(iv);

            // cipher text is remainder of message
            byte[] cipherText = Arrays.copyOfRange(bb.array(), bb.position(), bb.capacity());

            byte[] plainText = doDecrypt(cipherText, secretKey, iv);

            // decode the unencrypted payload into fields
            ByteBuffer payload = ByteBuffer.wrap(plainText);
            short nameLength = payload.getShort();
            byte[] nameBuf = new byte[nameLength];
            payload.get(nameBuf);
            // we don't know what the destination is
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
}
