package com.thebuildingblocks.derec.v0_9.httpprototype;

import com.thebuildingblocks.derec.v0_9.interfaces.DeRecId;
import com.thebuildingblocks.derec.v0_9.interfaces.DeRecSharer;
import com.thebuildingblocks.derec.v0_9.interfaces.DeRecStatusNotification;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.function.Consumer;

public class Sharer implements DeRecSharer{
    public static final List<String> availableVersions = List.of("0.9");
    public static Util.RetryParameters defaultRetryParameters = new Util.RetryParameters();
    // TODO: these should be grouped as "threshold management parameters" or some such
    public static int defaultThresholdRecovery = 3;
    public static int defaultHelpersRequiredForDeletion = 4; // number of helpers that need to confirm a new share

    private final Map<UUID, Secret> secrets; // a map of secret id to each secret that the sharer wishes to share
    public DeRecId id; // sharer's id
    public KeyPair keyPair; // public/private key pair
    // before deleting an old share
    public X509Certificate certificate; // certificate to use
    private Consumer<DeRecStatusNotification> listener = n -> {}; // do nothing

    /**
     * Hidden constructor
     */
    private Sharer() {
        secrets = new HashMap<>();
    }

    /**
     * Create a new secret with a random UUID as identifier
     *
     * @param bytesToProtect the content of the secret
     * @return a new secret
     */
    @Override
    public Secret newSecret(String description, byte[] bytesToProtect, List<DeRecId> helperIds) {
        UUID secretId = UUID.randomUUID();
        return newSecret(secretId, description, bytesToProtect, helperIds);
    }

    /**
     * Create a new secret with default values for its parameters. Block until paring and update are complete.
     *
     * @param secretId       the id of the secret
     * @param bytesToProtect the content of the secret
     * @return a newly shared secret
     */
    @Override
    public Secret newSecret(UUID secretId, String description, byte[] bytesToProtect, List<DeRecId> helperIds) {
        if (secrets.containsKey(secretId)) {
            throw new IllegalStateException("Secret with that Id already exists");
        }
        Secret secret = Secret.newBuilder()
                .sharerId(this.id)
                .secretId(secretId)
                .description(description)
                .storageRequired(bytesToProtect.length)// assume that secret does not grow in size
                .notificationListener(listener)
                .thresholdSecretRecovery(defaultThresholdRecovery)
                .thresholdForDeletion(defaultHelpersRequiredForDeletion)
                .build();
        secrets.put(secretId, secret);
        // add helpers and block
        secret.addHelpers(helperIds);
        // share secret and block
        secret.update(bytesToProtect);
        return secret;
    }

    @Override
    public Secret getSecret(UUID secretId) {
        return secrets.get(secretId);
    }

    @Override
    public List<Secret> getSecrets() {
        return secrets.values().stream().toList();
    }

    public void close() {
        for (Secret secret: secrets.values()) {
            secret.close();
        }
    }
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * A builder for a sharer
     */
    public static class Builder {
        private final Sharer sharer = new Sharer();

        public Builder id(DeRecId id) {
            sharer.id = id;
            return this;
        }

        public Builder keyPair(KeyPair keyPair) {
            sharer.keyPair = keyPair;
            return this;
        }

        public Builder x509Certificate(X509Certificate x509Certificate) {
            sharer.certificate = x509Certificate;
            return this;
        }

        public Builder notificationListener(Consumer<DeRecStatusNotification> listener) {
            sharer.listener = listener;
            return this;
        }

        public Sharer build() {
            return sharer;
        }
    }
}
