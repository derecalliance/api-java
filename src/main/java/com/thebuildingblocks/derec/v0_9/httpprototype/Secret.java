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

package com.thebuildingblocks.derec.v0_9.httpprototype;

import com.thebuildingblocks.derec.v0_9.interfaces.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.net.http.HttpClient;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static com.thebuildingblocks.derec.v0_9.httpprototype.Sharer.defaultRetryParameters;
import static com.thebuildingblocks.derec.v0_9.interfaces.DeRecStatusNotification.NotificationSeverity.ERROR;
import static com.thebuildingblocks.derec.v0_9.interfaces.DeRecStatusNotification.StandardNotificationType.UPDATE_FAILED;

/**
 * Implements the idea of Helper Controller, which coordinates helpers in respect of a single secret
 * <p>
 * The class is not intended to be thread-safe, i.e. pairing and update operations are not intended to be
 * safe to be called from different threads, however asynchronous completions carried out in different threads
 * are intended to be safe.
 */
public class Secret implements Closeable, DeRecSecret {
    /* -- basic details of the secret -- */
     Sharer sharer;
     private byte[] secretId; // the ID of the secret
     List<Consumer<DeRecStatusNotification>> notificationListeners = new ArrayList<>(); // listeners for events

    /* -- interoperability for the secret -- */
    int storageRequired; // bytes required to store shares of this secret
    int thresholdSecretRecovery; // how many helpers needed to recover the secret
    int thresholdForDeletion; // how many confirmations of new secret needed to be confirmed to delete old one
    final Util.RetryParameters retryParameters; // Retry parameters needed for this secret
    private final HttpClient httpClient;

    /* -- working variables -- these need to be thread safe */
    AtomicInteger latestShareVersion = new AtomicInteger();
    NavigableMap<Integer, Version> versions = Collections.synchronizedNavigableMap(new TreeMap<>());
    List<HelperClient> helpers = Collections.synchronizedList(new ArrayList<>(5)); // helpers with whom this secret is to be / was shared
    boolean closed;
    Logger logger = LoggerFactory.getLogger(this.getClass());

    Secret() {
        this(Util.RetryParameters.DEFAULT);
    }

    Secret(Util.RetryParameters retryParameters) {
        this.retryParameters = retryParameters;
        httpClient = HttpClient.newBuilder()
                .connectTimeout(retryParameters.getConnectTimeout())
                .build();
    }

    /**
     * Add helpers to the secret, pair and block for outcome.
     * @param helperIds the ids of the helpers to add
     */
    @Override
    public void addHelpers(List<? extends DeRecHelperInfo> helperIds) {
        // block for completion
        List<CompletableFuture<? extends DeRecHelperStatus>> futures = addHelpersAsync(helperIds);
        try {
            logger.trace("Awaiting result of pairing");
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[helperIds.size()])).get(retryParameters.pairingWaitSecs, TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            logger.error("Error while waiting for pairing completion");
            for (CompletableFuture<? extends DeRecHelperStatus> f: futures) {
                try {
                    logger.info("{} {} {}", f.isDone(), f.isDone() ? f.get().getId().getName():"?", f.isCompletedExceptionally());
                } catch (InterruptedException | ExecutionException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    /**
     * Add helpers to the secret and initiate pairing. The returned list of futures can be examined for success.
     *
     * @param helperIds the ids of the helpers to add
     * @return a list of {@link Future<HelperClient>} for helpers created and added
     */
    @Override
    public List<CompletableFuture<? extends DeRecHelperStatus>> addHelpersAsync(List<? extends DeRecHelperInfo> helperIds) {
        if (isClosed()) {
            throw new IllegalStateException("Cannot add helpers to closed secret");
        }
        List<CompletableFuture<? extends DeRecHelperStatus>> addedHelpers = new ArrayList<>();
        for (DeRecHelperInfo helperId: helperIds) {
            HelperClient helper = new HelperClient(this, helperId, httpClient, this.retryParameters);
            // todo other helper configuration, timeouts, etc.
            if (helpers.contains(helper)) {
                // todo is this true?
                throw new IllegalStateException("Cannot have the same helper more than once for a secret");
            }
            helpers.add(helper);
            logger.trace("Pairing {}", helperId.getName());
            helper.pair();
            addedHelpers.add(helper.pairingFuture);
        }
        return addedHelpers;
    }

    @Override
    public void removeHelpers(List<? extends DeRecHelperInfo> helperIds) {
        // update status of to-be-removed helpers
        // figure out the remaining threshold and re-share to those that remain
        // notify the removed helpers that they are removed
        // TODO
    }

    @Override
    public List<CompletableFuture<? extends DeRecHelperStatus>> removeHelpersAsync(List<? extends DeRecHelperInfo> helperIds) {
        // update status of to-be-removed helpers
        // figure out the remaining threshold and re-share to those that remain
        // notify the removed helpers that they are removed
        // TODO
        throw new UnsupportedOperationException();
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
        return update(bytesToProtect, null);
    }

    @Override
    public Version update(byte[] bytesToProtect, String description) {
        int pairedHelperCount = getPairedHelpers();
        try {
            logger.trace("Waiting for update future, {} paired helpers", pairedHelperCount);
            return updateAsync(bytesToProtect).get(retryParameters.timeout.getSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            notifyStatus(Notification.newBuilder()
                    .secret(this)
                    .message("Update timed out")
                    .severity(ERROR)
                    .build(UPDATE_FAILED));
        }
        return versions.lastEntry().getValue();
    }

    /**
     * Update a secret asynchronously.
     * @param bytesToProtect the content of the secret
     * @return a Future for this version
     */
    @Override
    public Future<Version> updateAsync(byte[] bytesToProtect) {
        return updateAsync(bytesToProtect, null);
    }

    @Override
    public Future<Version> updateAsync(byte[] bytesToProtect, String description) {
        logger.trace("Updating secret {}", Util.asUuid(secretId));
        if (isClosed()) {
            throw new IllegalStateException("Cannot update closed secret");
        }
        // cancel any in-progress update
        Version lastVersion = this.versions.get(latestShareVersion.get());
        if (!Objects.isNull(lastVersion) && !lastVersion.future.isDone()) {
            logger.info("Cancelling update of version {}", lastVersion.versionNumber);
            lastVersion.future.cancel(true);
        }

        String newDescription = Objects.nonNull(description) ? description :
                Objects.nonNull(lastVersion) ? lastVersion.description : null;

        // get a list of helpers thought to be active
        List<HelperClient> pairedHelpers = helpers.stream().filter(h -> h.status.equals(DeRecHelperStatus.PairingStatus.PAIRED)).toList();
        if (pairedHelpers.size() < thresholdSecretRecovery) {
            notifyStatus(Notification.newBuilder()
                    .secret(this)
                    .type(DeRecStatusNotification.StandardNotificationType.SECRET_UNAVAILABLE)
                    .severity(ERROR)
                    .message(String.format("Not enough helpers - %d paired, % d needed", pairedHelpers.size(), thresholdSecretRecovery))
                    .build());
        }

        // go to next version number
        int versionNumber = latestShareVersion.incrementAndGet();
        Version version = new Version(this, bytesToProtect, versionNumber, newDescription);
        this.versions.put(versionNumber, version);
        version.share(this.thresholdSecretRecovery, pairedHelpers);
        if (pairedHelpers.isEmpty()) {
            version.future.complete(version);
        }
        return version.future;
    }

    @Override
    public NavigableMap<Integer, ? extends DeRecVersion> getVersions() {
        return versions;
    }

    /**
     * Cancel all outstanding requests, unpair with all helpers and deactivate
     */
    @Override
    public CompletableFuture<Secret> closeAsync() {
        closed = true;
        for (Version version: versions.values()) {
            version.close();
        }
        List<CompletableFuture<? extends DeRecHelperStatus>> removedHelpers = new ArrayList<>();
        for (HelperClient helperClient: this.helpers) {
            // TODO helper.closeAsync();
            helperClient.unPair("Closing down");
            removedHelpers.add(helperClient.pairingFuture);
        }
        return CompletableFuture.allOf(removedHelpers.toArray(new CompletableFuture[0]))
                .thenApply(a -> this);
    }

    /**
     * Cancel all outstanding requests, unpair with all helpers and deactivate
     */
    @Override
    public void close() {
        closed = true;
        for (Version version: versions.values()) {
            version.close();
        }
        for (HelperClient helper: helpers) {
            helper.close();
        }
        // todo: how do you shut down httpclient?
    }

    public int getPairedHelpers() {
        return (int) helpers.stream()
                .filter(h -> h.status.equals(DeRecHelperStatus.PairingStatus.PAIRED))
                .count();
    }
    /**
     * is the secret available for sharing - are there active helpers?
     * @return true if secrets can be shared
     */
    @Override
    public boolean isAvailable() {
        return !isClosed() && helpers.stream()
                .filter(h -> h.status.equals(DeRecHelperStatus.PairingStatus.PAIRED))
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
    public byte[] getSecretId() {
        return secretId;
    }

    public UUID getSecretIdAsUuid() {
        return Util.asUuid(secretId);
    }

    public void notifyStatus(DeRecStatusNotification notification) {
        for (Consumer<DeRecStatusNotification> listener: notificationListeners) {
            listener.accept(notification);
        }
    }

    public static Builder newBuilder() {

        return new Secret.Builder();
    }

    public static class Builder {
        Secret secret = new Secret(defaultRetryParameters);

        public Builder sharer(Sharer sharer) {
            secret.sharer = sharer;
            return this;
        }

        public Builder secretId(byte[] secretId) {
            secret.secretId = secretId;
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

        public Builder notificationListener(Consumer<DeRecStatusNotification> listener) {
            secret.notificationListeners.add(listener);
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
