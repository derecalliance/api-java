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

import com.thebuildingblocks.derec.crypto.aesgcm.Message.PairingRequest;
import com.thebuildingblocks.derec.crypto.aesgcm.Message.PairingResponse;
import com.thebuildingblocks.derec.crypto.aesgcm.Counterparty.Helper;
import com.thebuildingblocks.derec.crypto.aesgcm.Counterparty.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.thebuildingblocks.derec.crypto.aesgcm.Crypto.keyPairGenerator;

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
