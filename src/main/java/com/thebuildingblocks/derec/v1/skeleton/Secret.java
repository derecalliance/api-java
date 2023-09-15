package com.thebuildingblocks.derec.v1.skeleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.net.http.HttpClient;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Implements the idea of Helper Controller, which coordinates helpers in respect of a single secret
 */
public class Secret implements Closeable {
    /* -- basic details of the secret -- */
    public DeRecId sharerId;
    public String secretId; // the ID of the secret
    public String description; // human-readable description of the secret

    /* -- interoperability for the secret -- */
    int storageRequired; // bytes required to store shares of this secret
    int thresholdSecretRecovery; // how many helpers needed to recover the secret
    int thresholdForDeletion; // how many confirmations of new secret needed to be confirmed to delete old one
    Util.RetryParameters retryParameters; // Retry parameters needed for this secret
    HttpClient httpClient = HttpClient.newHttpClient();

    /* -- working variables -- */
    /* todo these need to be thread safe */
    int latestShareVersion;
    NavigableMap<Integer, Version> versions = Collections.synchronizedNavigableMap(new TreeMap<>());
    List<HelperClient> helpers = new ArrayList<>(5); // helpers with whom this secret is to be / was shared

    Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Add helpers to the secret, pair and block for outcome. The returned list of helpers can be examined for
     * success or otherwise of the addition.
     * @param helperIds the ids of the helpers to add
     * @return a list of {@link HelperClient} created and added
     */
    public List<HelperClient> addHelpers(List<DeRecId> helperIds) throws ExecutionException, InterruptedException {
        return addHelpers(helperIds, false);
    }

    /**
     * Add helpers to the secret and initiate pairing. The returned list of helpers can be examined for
     * success or otherwise of the addition as well as a future to complete on success or failure.
     * @param helperIds the ids of the helpers to add
     * @param async true to add the helpers asynchronously
     * @return a list of {@link HelperClient} created and added
     */
    public List<HelperClient> addHelpers(List<DeRecId> helperIds, boolean async) throws ExecutionException, InterruptedException {
        List<HelperClient> addedHelpers = new ArrayList<>();
        for (DeRecId helperId: helperIds) {
            HelperClient helper = new HelperClient(this, helperId);
            // todo other helper configuration, timeouts, etc.
            if (helpers.contains(helper)) {
                // todo is this true?
                throw new IllegalStateException("Cannot have the same helper more than once for a secret");
            }
            helpers.add(helper);
            logger.info("Pairing {}", helper.helperId.name);
            helper.pair();
            // block for completion if requested
            if (!async) {
                helper.pairingFuture.get();
                logger.info("Pair {} {}", helper.helperId.name, helper.status);
            }
        }
        return addedHelpers;
    }

    public void removeHelpers(HelperClient... helper) {
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
    public Version update(byte []  bytesToProtect) {
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
    public Future<Version> updateAsync(byte []  bytesToProtect) {
        Version lastVersion = this.versions.get(latestShareVersion);
        if (!Objects.isNull(lastVersion) && !lastVersion.future.isDone()) {
            logger.info("Cancelling update of version {}", lastVersion.versionNumber);
            lastVersion.future.cancel(true);
        }


        List<HelperClient> activeHelpers = helpers.stream().filter(h -> h.status.equals(HelperClient.Status.PAIRED)).toList();
        if (activeHelpers.size() < thresholdSecretRecovery) {
            throw new IllegalStateException(String.format("Not enough helpers %d", activeHelpers.size()));
        }
        latestShareVersion++;
        this.versions.put(latestShareVersion, new Version(this, bytesToProtect, latestShareVersion, activeHelpers.size()));
        // add the shares to the list
        int shareNumber = 0;
        for (HelperClient helper: activeHelpers) {
            Share share = versions.get(latestShareVersion).shares.get(shareNumber++);
            helper.send(share);
        }
        return this.versions.get(latestShareVersion).future;
    }

    /**
     * Unpair with all helpers and deactivate
     */
    public void close() {
        for (HelperClient helper: helpers) {
            helper.close();
        }
    }

    /**
     * is the secret available for sharing - are there active helpers?
     * @return true if secrets can be shared
     */
    public boolean isAvailable() {
        return helpers.stream()
                .filter(h -> h.status.equals(HelperClient.Status.PAIRED))
                .count() >= this.thresholdSecretRecovery;
    }

    /**
     * List helpers and theie status
     * @return a printable list
     */
    public String listHelpers() {
        return helpers.stream()
                .map(h -> h.helperId.name + ": " + h.status.name())
                .collect(Collectors.joining("\n"));
    }

    /**
     * Representing a version of a share
     */
    public static class Version {
        final byte[] bytesToProtect; // copy of the secret when the version  was creates
        public Secret secret;
        // version numbers to be allocated so that they are always larger than the last shared
        int versionNumber;
        int threshold; // share threshold
        List<Share> shares; // the shares created for this version
        // a future to complete when the sharing is complete successfully or is known to have failed
        CompletableFuture<Version> future = new CompletableFuture<>();
        // whether it succeeded or failed
        boolean success;
        // count of update requests sent
        int updateRequestSent;
        // count of update replies received
        int successfulUpdateRepliesReceived;
        int failedUpdateReplyReceived;

        public Version(Secret secret, byte[] bytesToProtect, int latestShareVersion, int numShares) {
            this.versionNumber = latestShareVersion;
            this.secret = secret;
            this.bytesToProtect = bytesToProtect;
            this.shares = createShares(bytesToProtect, numShares);
        }

        List<Share> createShares(byte [] bytesToProtect, int numShares) {
           return IntStream.range(0, numShares).mapToObj(i -> new Share(bytesToProtect, this)).toList();
        }
    }

    /**
     * A share of a secret for a helper
     */
    public static class Share {
        final Version version;
        public CompletableFuture<Share> future;
        byte [] shareContent; // contents of the share
        Util.RetryStatus retryStatus = new Util.RetryStatus();
        HelperClient helper; // the helper who was sent this share
        ZonedDateTime confirmed; // when/whether the share was confirmed
        ZonedDateTime verified; // when the share was last verified

        public Share(byte[] shareContent, Version version) {
            this.version = version;
            this.shareContent = shareContent;
        }
    }
}
