package com.thebuildingblocks.derec.v1.prototype;

import java.net.URI;
import java.util.Objects;

/**
 * An identity for DeRec
 */
public class DeRecId {

    public static DeRecId[] DEFAULT_IDS = {
            new DeRecId("leemon", "mailto:leemon@swirldslabs.com", "http://localhost:8080/leemon"),
            new DeRecId("rohit", "mailto:rohit@swirldslabs.com", "http://localhost:8080/rohit"),
            new DeRecId("jo", "mailto:jorabin@thebuildingblocks.com", "http://localhost:8080/jo"),
            new DeRecId("niall", "mailto:niall@thebuildingblocks.com", "http://localhost:8080/niall"),
            new DeRecId("daniel", "mailto:daniel@thebuildingblocks.com", "http://localhost:8080/daniel")
    };

    String name;
    URI contact; // how to contact me outside the protocol, an email address, for example
    URI address; // my transport address

    public DeRecId(String name, String contact, String address) {
        this.name = name;
        this.contact = URI.create(contact);
        this.address = Objects.isNull(address) ? null : URI.create(address);
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
