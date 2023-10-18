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
import com.thebuildingblocks.derec.v0_9.interfaces.DeRecSecret;
import com.thebuildingblocks.derec.v0_9.interfaces.DeRecSharer;

import java.util.*;
import java.util.stream.Collectors;

/**
 * utilities for doing useful things
 */
public class Recipes {

    /**
     * Get a printable helpers and their status for a Secret
     * @return a printable list
     */
    public String listHelpers(DeRecSecret secret) {
        return secret.getHelpers().stream()
                .map(h -> h.getId().getName() + ": " + h.getStatus().name())
                .collect(Collectors.joining("\n"));
    }

    /**
     * Get a list of versions of a secret
     */
    public String listVersions(DeRecSecret secret) {
        return secret.getVersions().entrySet().stream().
                map(e -> e.getKey() + ": " + e.getValue().isProtected()).
                collect(Collectors.joining("\n"));
    }

    /**
     * Get a list of all helper ids and the secrets they protect
     */
    public static Map<DeRecId, List<DeRecSecret>> listHelpers(DeRecSharer sharer) {
        // todo: this could possibly be done more elegantly using mapping collector
        // create a map of DeRecId and List<Secret>
        final Map<DeRecId, List<DeRecSecret>> secretMap = new HashMap<>();
        // populate the map
        sharer.getSecrets().forEach(s -> s.getHelpers()
                .forEach(h -> {
                    if (!secretMap.containsKey(h.getId())) {
                        secretMap.put(h.getId(), new ArrayList<>());
                    }
                    secretMap.get(h.getId()).add(s);
                }));
        return secretMap;
    }
}
