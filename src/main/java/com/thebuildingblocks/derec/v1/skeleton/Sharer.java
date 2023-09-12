package com.thebuildingblocks.derec.v1.skeleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class Sharer {
    public static final List<String> availableVersions = List.of("0.9");
    public DeRecId id; // sharer's id
    public KeyPair keyPair; // public/private key pair
    public X509Certificate certificate; // certificate to use
    private final Map<String, Secret> secrets; // a map of secret id to each secret that the sharer wishes to share

    public static Util.RetryParameters defaultRetryParameters = new Util.RetryParameters();
    // TODO: these should be grouped as "threshold management parameters" or some such
    public static int defaultThresholdRecovery = 3;
    public static int defaultHelpersRequiredForDeletion = 4; // number of helpers that need to confirm a new share
                                                      // before deleting an old share

    static Logger logger = LoggerFactory.getLogger(Sharer.class);
    /**
     * Hidden constructor per Joshua Bloch
     */
    private Sharer () {
        secrets = new HashMap<>();
    }

    /**
     * Create a new secret with a random UUID as identifier
     * @param bytesToProtect the content of the secret
     * @return a new secret
     */
    public Secret newSecret(String description, byte [] bytesToProtect, List<DeRecId> helperIds) throws ExecutionException, InterruptedException {
        String secretId = UUID.randomUUID().toString();
        return newSecret(secretId, description, bytesToProtect, helperIds);
    }

    /**
     * Create a new secret with default values for its parameters
     * @param secretId the id of the secret
     * @param bytesToProtect the content of the secret
     * @return a new secret
     */
    public Secret newSecret(String secretId, String description, byte [] bytesToProtect, List<DeRecId> helperIds) throws ExecutionException, InterruptedException {
        if (secrets.containsKey(secretId)) {
            throw new IllegalStateException("Secret with that Id already exists");
        }
        // todo make this a builder maybe
        Secret secret = new Secret();
        secret.sharerId = this.id;
        secret.secretId = secretId;
        secret.description = description;
        secret.secret = bytesToProtect;
        secret.storageRequired = bytesToProtect.length; // assume that secret does not grow in size
        secret.thresholdSecretRecovery = defaultThresholdRecovery;
        secret.thresholdForDeletion = defaultHelpersRequiredForDeletion;
        secret.retryParameters = defaultRetryParameters.clone();
        List<HelperClient> helperClients = secret.addHelpers(helperIds);
        if (!secret.isAvailable()) {
            System.err.println(secret.listHelpers());
            throw new IllegalStateException("Not enough helpers available to share secret");
        }
        secret.share();
        secrets.put(secretId, secret);
        return secret;
    }

    public Secret getSecret(String secretId) {
        return secrets.get(secretId);
    }

    public List<String> getSecrets() {
        return secrets.keySet().stream().toList();
    }

    /**
     * A builder for a sharer
     */
    public static class Builder {
        private final Sharer sharer = new Sharer();

        public Builder id (DeRecId id) {
            sharer.id = id;
            return this;
        }

        public Builder keyPair (KeyPair keyPair) {
            sharer.keyPair = keyPair;
            return this;
        }

        public Builder x509Certificate (X509Certificate x509Certificate) {
            sharer.certificate = x509Certificate;
            return this;
        }

        public Sharer build () {
            return sharer;
        }
    }

    public static void main(String [] args) throws ExecutionException, InterruptedException {
        Sharer me = new Sharer.Builder()
                .id(new DeRecId("Secret Sammy", "mailto:test@example.org", null))
                .build();
        Secret secret = me.newSecret("Martin Luther", "I have a dream".getBytes(StandardCharsets.UTF_8), Arrays.asList(DeRecId.DEFAULT_IDS));
        Secret.Version v = secret.update("I have another dream".getBytes(StandardCharsets.UTF_8));
        logger.info("Secret version {}, {}", v.versionNumber, v.success);
        secret.close();
    }
}
