package com.thebuildingblocks.derec;

import com.thebuildingblocks.derec.hse.Message.PairingRequest;
import com.thebuildingblocks.derec.hse.Message.PairingResponse;
import com.thebuildingblocks.derec.hse.Counterparty.Helper;
import com.thebuildingblocks.derec.hse.Counterparty.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.thebuildingblocks.derec.hse.Crypto.keyPairGenerator;

/**
 * Prototype of initial AES-GCM based pairing and encryption proposal for DeRec
 */
public class AESGCM {
    static Logger logger = LoggerFactory.getLogger(AESGCM.class);
    public static void main(String[] args) {
        logger.info("Key pair algorithm {}", keyPairGenerator.getAlgorithm());
        // alice creates a pairing request
        User alice = new User("alice");
        PairingRequest pairingRequest = alice.createPairingRequest("bob");
        byte [] pairingRequestMessage = alice.serialize(pairingRequest);

        // bob receives pairing request and creates pairing response
        Helper bob = new Helper("bob");
        PairingRequest incomingPairingRequest = PairingRequest.deserialize(pairingRequestMessage);
        logger.info("Bob: Incoming pairing request from {}", incomingPairingRequest.originatorName);
        PairingResponse outgoingPairingResponse = bob.createPairingResponse(incomingPairingRequest);
        byte [] pairingResponseMessage = bob.serialize(outgoingPairingResponse);

        // alice receives pairing response
        PairingResponse incomingPairingResponse = PairingResponse.deserialize(pairingResponseMessage,
                // alice has to know it is from "bob" otherwise she can't decrypt (the encrypted part
                // contains the fact that is from bob)
                alice.cpDetails.get("bob").getPrivate());
        logger.info("Alice: Incoming pairing response from {}", incomingPairingResponse.originatorName);
    }
}
