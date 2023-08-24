package com.thebuildingblocks.derec.model;

import java.net.URI;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

/**
 * Information a sharer knows about itself
 */
public class Sharer {
    Util.DeRecId id; // sharer's id
    KeyPair keyPair; // public/private key pair
    X509Certificate certificate; // certificate to use
    Map<String, Secret> secrets; // a map of secret id to each secret that the sharer wishes to share
    URI contact; // a way of contacting me
    List<String> availableVersions; // a list of available protocol versions

    /**
     * information about a secret that is to be or has been shared
     */
    public static class Secret {
        String secretId; // the ID of the secret
        List<SecretHelper> helpers; // helpers with whom this secret is to be / was shared
        byte [] secret; // content of the secret
        int storageRequired; // bytes required to store shares of this secret
        int thresholdSecretRecovery; // how many helpers needed to recover the secret
        int thresholdForDeletion; // how many confirmations of new secret needed to delete old one
        Util.RetryParameters retryParameters; // Retry parameters needed for this secret
    }

    /**
     * Sharer's view of a helper for a single secret, there will be multiple entries for the
     * same helper - one for each secret that have shared to the,
     */
    public static class SecretHelper {
        public enum Status {
            INVITED, // no reply yet
            PAIRED, // replied positively
            REFUSED, // replied negatively
            GONE // timeout, disconnect etc.
        }
        Util.RetryStatus retryStatus; // some kind of way of figuring out retry and timeout
        int lastConfirmedShareVersion; // which version has the helper got?
        Util.DeRecId helperId; // unique Id for helper
        URI tsAndCs;    // link to legal conditions regarding what the helper is to do about
                        // authentication for recovery and substitution of sharer
        URI uri; // a way of contacting the helper
        PublicKey publicKey; // public key for the helper (for this secret may be the same for all secrets for this sharer)
        X509Certificate certificate; // The helper's certificate
        String protocolVersion; // accepted protocol version
        List<SecretShare> shares; // a list of the shares sent to this helper
    }

    /**
     * A share of a secret for a helper
     */
    public static class SecretShare {
        byte [] shareContent; // contents of the share
        int shareVersion; // the share version number
        int threshold; // share threshold
        ZonedDateTime confirmed; // when/whether the share was confirmed
        ZonedDateTime verified; // when the share was last verified
    }
}
