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

package org.derecalliance.derec.api;

import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Objects;

/**
 * Information about the identity of a helper or a sharer
 */
public class DeRecIdentity {
    private static final MessageDigest messageDigest;

    static {
        try {
            messageDigest = MessageDigest.getInstance("SHA-384");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private final String name; // human-readable identification
    private final URI contact; // how to contact me outside the protocol, an email address, for example
    private final URI address; // transport address
    private final String publicEncryptionKey;
    private String publicSignatureKey;
    private final byte[] publicEncryptionKeyDigest;
    private byte[] publicSignatureKeyDigest;


    /**
     * Create a helper info
     * @param name human-readable name
     * @param contact contact address - e.g. email
     * @param address DeRec address
     * @param publicEncryptionKey PEM encoded public encryption key
     * @param publicSignatureKey PEM encoded public signature key
     */
    public DeRecIdentity(String name, String contact, String address, String publicEncryptionKey, String publicSignatureKey) {
        this.name = name;
        this.contact = URI.create(contact);
        this.address = Objects.isNull(address) ? null : URI.create(address);
        this.publicEncryptionKey = publicEncryptionKey;
        this.publicSignatureKey = publicSignatureKey;
        this.publicEncryptionKeyDigest = messageDigest.digest(Base64.getDecoder().decode(publicEncryptionKey));
        if (publicSignatureKey != null) {
            // This check is necessary because when pairing with someone, we don't know their public signature key immediately
            this.publicSignatureKeyDigest = messageDigest.digest(Base64.getDecoder().decode(publicSignatureKey));
        }
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
     * @return PEM encoded public encryption key
     */
    public String getPublicEncryptionKey() {
        return publicEncryptionKey;
    }

    /**
     * @return PEM encoded public signature key
     */
    public String getPublicSignatureKey() {
        return publicSignatureKey;
    }

    /**
     * @return digest of public encryption key
     */
    public byte[] getPublicEncryptionKeyDigest() {
        return publicEncryptionKeyDigest;
    }

    /**
     * @return digest of public signature key
     */
    public byte[] getPublicSignatureKeyDigest() {
        return publicSignatureKeyDigest;
    }

    /**
     * Used to set a peer's public signature key during pairing
     * @param publicSignatureKey public signature key
     */
    public void setPublicSignatureKey(String publicSignatureKey) {
        this.publicSignatureKey = publicSignatureKey;
        this.publicSignatureKeyDigest = messageDigest.digest(Base64.getDecoder().decode(publicSignatureKey));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeRecIdentity deRecId)) return false;
        return Objects.equals(getName(), deRecId.getName()) &&
                Objects.equals(getContact(), deRecId.getContact()) &&
                Objects.equals(getAddress(), deRecId.getAddress()) &&
                Objects.equals(getPublicEncryptionKey(), deRecId.getPublicEncryptionKey()) &&
                Objects.equals(getPublicSignatureKey(), deRecId.getPublicSignatureKey());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getContact(), getAddress(), getPublicEncryptionKey(), getPublicSignatureKey());
    }
}
