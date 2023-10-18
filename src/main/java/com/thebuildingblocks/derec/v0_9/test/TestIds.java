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

package com.thebuildingblocks.derec.v0_9.test;

import com.thebuildingblocks.derec.v0_9.interfaces.DeRecId;
import derec.message.Derecmessage;

import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;

import static com.thebuildingblocks.derec.v0_9.httpprototype.Cryptography.keyPairGenerator;

public class TestIds {
    public static DeRecId[] DEFAULT_IDS = {
            new DeRecId("leemon", "mailto:leemon@swirldslabs.com", "http://localhost:8080/leemon"),
            new DeRecId("rohit", "mailto:rohit@swirldslabs.com", "http://localhost:8080/rohit"),
            new DeRecId("dipti", "mailto:dipti@swirldslabs.com", "http://localhost:8080/dipti"),
            new DeRecId("cate", "mailto:cate@swirldslabs.com", "http://localhost:8080/cate"),
            new DeRecId("jo", "mailto:jo@thebuildingblocks.com", "http://localhost:8080/jo"),
            new DeRecId("niall", "mailto:niall@thebuildingblocks.com", "http://localhost:8080/niall"),
            new DeRecId("daniel", "mailto:daniel@thebuildingblocks.com", "http://localhost:8080/daniel"),
            new DeRecId("noone", "mailto:noone@thebuildingblocks.com", "http://localhost:8080/noone"),
            new DeRecId("nowhere", "mailto:nowhere@thebuildingblocks.com", "http://192.168.1.40/nowhere"),
    };

    public static Map<String, KeyPair> DEFAULT_KEYPAIRS = new HashMap<>();

    static {
        for (DeRecId id: DEFAULT_IDS) {
            DEFAULT_KEYPAIRS.put(id.getName(), keyPairGenerator.generateKeyPair());
        }
    }
}
