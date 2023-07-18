package com.thebuildingblocks.derec.hse;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;

import static com.thebuildingblocks.derec.hse.Encryption.*;

public class Participant {

    // name of the participant
    String name;

    // a map of our private / public keys in respect of other participants
    public Map<String, KeyPair> keyPairMap = new HashMap<>();
    public Map<String, SecretKey> secretKeyMap = new HashMap<>();

    public static class User extends Participant {

        public User(String name) {
            this.name = name;
        }

        public Message.PairingRequest createPairingRequest(String destinationName) {
            // store a key pair for this helper
            keyPairMap.put(destinationName, kpg.generateKeyPair());
            return new Message.PairingRequest(name, keyPairMap.get(destinationName).getPublic());
        }

        public Message.PairingAck createPairingAck(Message.PairingResponse pairingResponse) {

            return new Message.PairingAck(null);
        }

        public byte[] serialise(Message.PairingRequest pairingRequest) {
            return pairingRequest.serialize();
        }
    }

    public static class Helper extends Participant {
        public Helper(String name) {
            this.name = name;
        }

        public Message.PairingResponse createPairingResponse(Message.PairingRequest pairingRequest) {
            // first store a key pair for this requester
            keyPairMap.put(pairingRequest.originatorName, kpg.generateKeyPair());
            // create the ecdh key
            byte[] ecdhKey = generateEcdhKey(keyPairMap.get(pairingRequest.originatorName).getPrivate(), pairingRequest.originatorPublicKey);
            SecretKey secretKey = generateSecretKey(ecdhKey);
            secretKeyMap.put(pairingRequest.originatorName, secretKey);
            return new Message.PairingResponse(name, pairingRequest.originatorName);
        }

        public byte [] serialize(Message.PairingResponse pairingResponse) {
            return pairingResponse.serialize(secretKeyMap.get(pairingResponse.destinationName),
                    keyPairMap.get(pairingResponse.destinationName).getPublic());
        }

        public void receivePairingAck(Message.PairingAck pairingAck) {

        }
    }
}
