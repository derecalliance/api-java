package com.thebuildingblocks.derec.model;

import java.net.URI;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Information a helper knows about itself
 */
public class Helper {
    Util.DeRecId id; // helper's Id
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
        Util.DeRecId sharerId; // sharer unique id
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
        byte [] shareContent; // contents of the share
        int shareVersion; // the version of the share
        byte[] signature; // the signature attached to the share
    }

}
