package com.thebuildingblocks.derec.v0_9.httpprototype;

import com.thebuildingblocks.derec.v0_9.interfaces.DeRecId;
import com.thebuildingblocks.derec.v0_9.interfaces.DeRecPairable;
import com.thebuildingblocks.derec.v0_9.interfaces.DeRecSecret;
import com.thebuildingblocks.derec.v0_9.interfaces.DeRecVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.net.http.HttpClient;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static com.thebuildingblocks.derec.v0_9.httpprototype.Sharer.defaultRetryParameters;

/**
 * Implements the idea of Helper Controller, which coordinates helpers in respect of a single secret
 * <p>
 * The class is not intended to be thread-safe, i.e. pairing and update operations are not intended to be
 * safe to be called from different threads, however asynchronous completions carried out in different threads
 * are intended to be safe.
 */
public class Secret implements Closeable, DeRecSecret {
    /* -- basic details of the secret -- */
     DeRecId sharerId;
     UUID secretId; // the ID of the secret
     String description; // human-readable description of the secret
     Consumer<StatusNotification> notificationListener; // listener for events

    /* -- interoperability for the secret -- */
    int storageRequired; // bytes required to store shares of this secret
    int thresholdSecretRecovery; // how many helpers needed to recover the secret
    int thresholdForDeletion; // how many confirmations of new secret needed to be confirmed to delete old one
    final Util.RetryParameters retryParameters; // Retry parameters needed for this secret
    final HttpClient httpClient;

    /* -- working variables -- these need to be thread safe */
    AtomicInteger latestShareVersion = new AtomicInteger();
    NavigableMap<Integer, Version> versions = Collections.synchronizedNavigableMap(new TreeMap<>());
    List<HelperClient> helpers = Collections.synchronizedList(new ArrayList<>(5)); // helpers with whom this secret is to be / was shared
    boolean closed;
    Logger logger = LoggerFactory.getLogger(this.getClass());

    Secret() {
        this(new Util.RetryParameters());
    }

    Secret(Util.RetryParameters retryParameters) {
        this.retryParameters = retryParameters;
        httpClient = HttpClient.newBuilder().connectTimeout(retryParameters.timeout).build();
    }


    /**
     * Add helpers to the secret, pair and block for outcome.
     * @param helperIds the ids of the helpers to add
     */
    @Override
    public void addHelpers(List<? extends DeRecId> helperIds) {
        // block for completion
        for (Future<? extends DeRecPairable> future: addHelpersAsync(helperIds)){
            try {
                DeRecPairable helper = future.get();
                logger.trace("Pair {} {}", helper.getId().getName(), helper.getStatus());
            } catch (InterruptedException | ExecutionException e) {
                logger.trace("Pairing exception:", e);
            }
        }
    }

    /**
     * Add helpers to the secret and initiate pairing. The returned list of futures can be examined for success.
     * @param helperIds the ids of the helpers to add
     * @return a list of {@link Future<HelperClient>} for helpers created and added
     */
    @Override
    public List<Future<? extends DeRecPairable>> addHelpersAsync(List<? extends DeRecId> helperIds) {
        if (isClosed()) {
            throw new IllegalStateException("Cannot add helpers to closed secret");
        }
        List<Future<? extends DeRecPairable>> addedHelpers = new ArrayList<>();
        for (DeRecId helperId: helperIds) {
            HelperClient helper = new HelperClient(this, helperId);
            // todo other helper configuration, timeouts, etc.
            if (helpers.contains(helper)) {
                // todo is this true?
                throw new IllegalStateException("Cannot have the same helper more than once for a secret");
            }
            helpers.add(helper);
            logger.trace("Pairing {}", helper.helperId.getName());
            helper.pair();
            addedHelpers.add(helper.pairingFuture);
        }
        return addedHelpers;
    }

    @Override
    public void removeHelpers(List<? extends DeRecId> helperIds) {
        // update status of to-be-removed helpers
        // figure out the remaining threshold and re-share to those that remain
        // notify the removed helpers that they are removed
    }


    /**
     * Update a secret synchronously. The call blocks until the thresholdSecretRecovery number of successful
     * confirmations is received from helpers, or until all confirmations have been received/failed with timeout.
     * <p>
     * The <code>success</code> field of the returned {@link Version} indicates success or otherwise
     * @param bytesToProtect the content of the secret
     * @return the updated secret
     */
    @Override
    public Version update(byte[] bytesToProtect) {
        try {
            logger.info("Updating secret");
            return updateAsync(bytesToProtect).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Update a secret asynchronously.
     * @param bytesToProtect the content of the secret
     * @return a Future for this version
     */
    @Override
    public Future<Version> updateAsync(byte[] bytesToProtect) {
        if (isClosed()) {
            throw new IllegalStateException("Cannot update closed secret");
        }
        // cancel any in-progress update
        Version lastVersion = this.versions.get(latestShareVersion.get());
        if (!Objects.isNull(lastVersion) && !lastVersion.future.isDone()) {
            logger.info("Cancelling update of version {}", lastVersion.versionNumber);
            lastVersion.future.cancel(true);
        }

        // get a list of helpers thought to be active
        List<HelperClient> activeHelpers = helpers.stream().filter(h -> h.status.equals(DeRecPairable.PairingStatus.PAIRED)).toList();
        if (activeHelpers.size() < thresholdSecretRecovery) {
            throw new IllegalStateException(String.format("Not enough helpers %d", activeHelpers.size()));
        }

        // go to next version number
        int version = latestShareVersion.incrementAndGet();
        this.versions.put(version, new Version(this, bytesToProtect, version, activeHelpers.size()));
        // add the shares to the list
        int shareNumber = 0;
        for (HelperClient helper: helpers) {
            Version.Share share = versions.get(version).shares.get(shareNumber++);
            helper.send(share);
        }
        return this.versions.get(version).future;
    }

    @Override
    public NavigableMap<Integer, ? extends DeRecVersion> getVersions() {
        return versions;
    }

    /**
     * Unpair with all helpers and deactivate
     */
    @Override
    public void close() {
        closed = true;
        for (HelperClient helper: helpers) {
            helper.close();
        }
    }

    /**
     * is the secret available for sharing - are there active helpers?
     * @return true if secrets can be shared
     */
    @Override
    public boolean isAvailable() {
        return !isClosed() && helpers.stream()
                .filter(h -> h.status.equals(DeRecPairable.PairingStatus.PAIRED))
                .count() >= this.thresholdSecretRecovery;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public List<HelperClient> getHelpers(){
        return this.helpers;
    }

    @Override
    public UUID getSecretId() {
        return secretId;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public static class Builder {
        Secret secret = new Secret(defaultRetryParameters.clone());

        public Builder sharerId(DeRecId id) {
            secret.sharerId = id;
            return this;
        }

        public Builder secretId(UUID secretId) {
            secret.secretId = secretId;
            return this;
        }

        public Builder description(String description) {
            secret.description = description;
            return this;
        }

        public Builder storageRequired(int length) {
            secret.storageRequired = length;
            return this;
        }

        public Builder thresholdSecretRecovery(int threshold) {
            secret.thresholdSecretRecovery = threshold;
            return this;
        }

        public Builder thresholdForDeletion(int threshold) {
            secret.thresholdForDeletion = threshold;
            return this;
        }

        public Builder notificationListener(Consumer<StatusNotification> listener) {
            secret.notificationListener = listener;
            return this;
        }

        public Secret build(){
            Secret copy = secret;
            // render builder unusable after this method
            secret = null;
            return copy;
        }

    }
}
