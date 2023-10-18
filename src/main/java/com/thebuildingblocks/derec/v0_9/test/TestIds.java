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

import com.thebuildingblocks.derec.v0_9.interfaces.DeRecHelperInfo;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static com.thebuildingblocks.derec.v0_9.httpprototype.Cryptography.keyPairGenerator;

public class TestIds {

    public static String[] helperNames = {"leemon", "rohit", "dipti", "cate", "jo", "niall", "daniel", "noone", "nowhere"};

    public static Map<String, KeyPair> DEFAULT_KEYPAIRS = new HashMap<>();

    static {
        for (String name: helperNames) {
            DEFAULT_KEYPAIRS.put(name, keyPairGenerator.generateKeyPair());
        }
    }

    private static String pem(String name){
        return Base64.getEncoder().encodeToString(DEFAULT_KEYPAIRS.get(name).getPublic().getEncoded());
    }

    public static String pemFrom(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }
    public static DeRecHelperInfo[] DEFAULT_IDS = {
            new DeRecHelperInfo("leemon", "mailto:leemon@swirldslabs.com", "http://localhost:8080/leemon", pem("leemon")),
            new DeRecHelperInfo("rohit", "mailto:rohit@swirldslabs.com", "http://localhost:8080/rohit", pem("rohit")),
            new DeRecHelperInfo("dipti", "mailto:dipti@swirldslabs.com", "http://localhost:8080/dipti", pem("dipti")),
            new DeRecHelperInfo("cate", "mailto:cate@swirldslabs.com", "http://localhost:8080/cate", pem("cate")),
            new DeRecHelperInfo("jo", "mailto:jo@thebuildingblocks.com", "http://localhost:8080/jo", pem("jo")),
            new DeRecHelperInfo("niall", "mailto:niall@thebuildingblocks.com", "http://localhost:8080/niall", pem("niall")),
            new DeRecHelperInfo("daniel", "mailto:daniel@thebuildingblocks.com", "http://localhost:8080/daniel", pem("daniel")),
            new DeRecHelperInfo("noone", "mailto:noone@thebuildingblocks.com", "http://localhost:8080/noone", pem("noone")),
            new DeRecHelperInfo("nowhere", "mailto:nowhere@thebuildingblocks.com", "http://192.168.1.40/nowhere", pem("nowhere")),
    };

}
