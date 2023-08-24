package com.thebuildingblocks.derec.crypto.aesgcm;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

/**
 * Representing a participant in the DeRec protocol. Can be a User (with secrets to share) or a Helper (which
 * stores shared secrets for a User).
 * <p>
 * Acts as a factory for the various messages that are originated by that role, mainly so that secrets (for
 * encryption etc.) only get stored in the class and are passed on the stack when used (rather than also being
 * stored in message classes)
 */
public class Counterparty {
    /**
     * Storing info about counterparties here, encryption, status etc.
     * <p>
     * TODO need to be concerned about storing secrets in a way that could be stolen from the JVM by an adversary
     */
    public static class CounterpartyRecord {
        // our public / private keys for this counterparty
        KeyPair keyPair;

        // our encryption key for this counterparty
        SecretKey secretKey;

        public CounterpartyRecord() {
            this.keyPair = Crypto.keyPairGenerator.generateKeyPair();
        }

        public PublicKey getPublic() {
            return this.keyPair.getPublic();
        }

        public PrivateKey getPrivate() {
            return this.keyPair.getPrivate();
        }
    }

    /* ---
    provisionally using a simple name string to identify counterparties, however we will likely need
    other things here, like an id of some kind, as well as a "friendly" name
    --- */
    private final String name;
    public Map<String, CounterpartyRecord> cpDetails = new HashMap<>();

    public Counterparty(String name) {
        // strings in the protocol are defined as being NFC normalized
        this.name = Normalizer.normalize(name, Normalizer.Form.NFC);
    }

    public String getName() {
        return name;
    }

    public static class User extends Counterparty {

        public User(String name) {
            super(name);
        }

        public Message.PairingRequest createPairingRequest(String destinationName) {
            // store a key pair for this helper
            cpDetails.put(destinationName, new CounterpartyRecord());
            return new Message.PairingRequest(getName(), cpDetails.get(destinationName).keyPair.getPublic());
        }

        public byte[] serialize(Message.PairingRequest pairingRequest) {
            return pairingRequest.serialize();
        }
    }

    public static class Helper extends Counterparty {

        public Helper(String name) {
            super(name);
        }

        public Message.PairingResponse createPairingResponse(Message.PairingRequest pairingRequest) {
            // first store a key pair for this requester
            cpDetails.put(pairingRequest.originatorName, new CounterpartyRecord());
            // create the secret
            byte[] secret = Crypto.generateSharedSecret(cpDetails.get(pairingRequest.originatorName).getPrivate(),
                    pairingRequest.originatorPublicKey);
            cpDetails.get(pairingRequest.originatorName).secretKey = Crypto.generateSecretKey(secret);
            return new Message.PairingResponse(getName(), pairingRequest.originatorName);
        }

        public byte[] serialize(Message.PairingResponse pairingResponse) {
            return pairingResponse.serialize(cpDetails.get(pairingResponse.destinationName).secretKey,
                    cpDetails.get(pairingResponse.destinationName).getPublic());
        }
    }
}
