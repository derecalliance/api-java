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
