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

package com.thebuildingblocks.derec.v0_9.interfaces;

import java.net.URI;
import java.util.Objects;

/**
 * Helper info serving also as a key to identify a helper instance
 */
public class DeRecHelperInfo {
    private final String name; // human-readable identification
    private final URI contact; // how to contact me outside the protocol, an email address, for example
    private final URI address; // my transport address
    private final String publicKey;

    /**
     * Create a helper info
     * @param name human-readable name
     * @param contact contact address - e.g. email
     * @param address DeRec address
     * @param publicKey PEM encoded public key
     */
    public DeRecHelperInfo(String name, String contact, String address, String publicKey) {
        this.name = name;
        this.contact = URI.create(contact);
        this.address = Objects.isNull(address) ? null : URI.create(address);
        this.publicKey = publicKey;
    }

    /**
     * @return human-readable name
     */
    public String getName() {
        return name;
    }

    /**
     * @return human readable contact info, e.g. email address
     */
    public URI getContact() {
        return contact;
    }

    /**
     * @return network address for DeRec protocol
     */
    public URI getAddress() {
        return address;
    }

    /**
     * @return public key of the helper
     */
    private String getPublicKey() {
        return publicKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeRecHelperInfo deRecId)) return false;
        return Objects.equals(getName(), deRecId.getName()) &&
                Objects.equals(getContact(), deRecId.getContact()) &&
                Objects.equals(getAddress(), deRecId.getAddress()) &&
                Objects.equals(getPublicKey(), deRecId.getPublicKey());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getContact(), getAddress(), getPublicKey());
    }
}
