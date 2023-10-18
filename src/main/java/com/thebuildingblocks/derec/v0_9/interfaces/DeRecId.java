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
 * An identity for DeRec
 */
public class DeRecId {
    String name; // human-readable identification
    URI contact; // how to contact me outside the protocol, an email address, for example
    URI address; // my transport address

    public DeRecId(String name, String contact, String address) {
        this.name = name;
        this.contact = URI.create(contact);
        this.address = Objects.isNull(address) ? null : URI.create(address);
    }

    public String getName() {
        return name;
    }

    public URI getContact() {
        return contact;
    }

    public URI getAddress() {
        return address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeRecId deRecId)) return false;
        return Objects.equals(name, deRecId.name) && Objects.equals(contact, deRecId.contact) && Objects.equals(address, deRecId.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, contact, address);
    }
}
