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

package com.thebuildingblocks.derec.v0_9.httpprototype;

import com.thebuildingblocks.derec.v0_9.interfaces.DeRecId;

import java.net.URI;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Stub implementation of a HelperServer, just responds OK
 */
public class HelperServer {

    /**
     * Placeholder for information a helper knows about itself
     */
    public static class HelperModel {
        DeRecId id; // helper's Id
        KeyPair keyPair; // public/private key pair
        X509Certificate certificate; // certificate to use
        URI address; // my transport address
        List<String> availableVersions; // a list of available protocol versions
        Util.RetryParameters retryParameters; // parameters to negotiate for any sharer/secret
        List<Sharer> sharer; // list of paired sharers

        /**
         * Information a helper know about a sharer
         */
        public static class Sharer {
            DeRecId sharerId; // sharer unique id
            URI sharerAddress; // sharer transport address
            PublicKey publicKey; // sharer's public key
            URI tsAndCs;    // link to legal conditions regarding what the helper is to do about
            // authentication for recovery and substitution of sharer
            List<Integer> keepList; // the shares to keep (these can come from any secret)
            List<Share> shares; // the kept shares
        }

        /**
         * Information a helper knows about a share
         */
        public static class Share {
            byte[] shareContent; // contents of the share
            int shareVersion; // the version of the share
            byte[] signature; // the signature attached to the share
        }
    }

}
