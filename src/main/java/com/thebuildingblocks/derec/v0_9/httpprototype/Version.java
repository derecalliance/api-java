package com.thebuildingblocks.derec.v0_9.httpprototype;

import com.thebuildingblocks.derec.v0_9.interfaces.DeRecSecret;
import com.thebuildingblocks.derec.v0_9.interfaces.DeRecStatusNotification;
import com.thebuildingblocks.derec.v0_9.interfaces.DeRecVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static com.thebuildingblocks.derec.v0_9.httpprototype.Version.ResultType.SHARE;
import static com.thebuildingblocks.derec.v0_9.httpprototype.Version.ResultType.VERIFY;
import static com.thebuildingblocks.derec.v0_9.interfaces.DeRecStatusNotification.Type.*;


/**
 * Representing a version of a secret
 */
public class Version implements DeRecVersion {
    // the secret this version belongs to
    final Secret secret;
    // version numbers to be allocated so that they are always larger than the last shared
    final int versionNumber;
    // the bytes of this secret
    byte[] protectedValue;
    // share threshold
    int recombinationThreshold;
    // the shares created for this version
    List<Share> shares;
    // a future to complete when the sharing is complete successfully or is known to have failed
    CompletableFuture<Version> future = new CompletableFuture<>();
    // place to hold the future for the verification process, this never completes
    private ScheduledFuture<?> verificationTimer;
    // executor for repeated verification
    ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);
    // result of the original version sharing and the latest verification
    Map<ResultType, ResultCount> resultCounts = new HashMap<>();

    Logger logger = LoggerFactory.getLogger(this.getClass());


    Version(Secret secret, int versionNumber) {
        this.versionNumber = versionNumber;
        this.secret = secret;
        for (ResultType r: ResultType.values()) {
            resultCounts.put(r, new ResultCount());
        }
    }

    @Override
    public long getVersionNumber() {
        return versionNumber;
    }

    @Override
    public DeRecSecret getSecret() {
        return secret;
    }

    @Override
    public byte[] getProtectedValue() {
        return protectedValue;
    }

    void notifyStatus (DeRecStatusNotification.Type notificationType, HelperClient helper, String message) {
        secret.notifyStatus(Notification.newBuilder()
                .secret(secret)
                .version(this)
                .pairable(helper)
                .message(message)
                .build(notificationType));
    }

    void notifyStatus (DeRecStatusNotification.Type notificationType) {
        secret.notifyStatus(Notification.newBuilder()
                .secret(secret)
                .version(this)
                .build(notificationType));
    }

    @Override
    public boolean isProtected() {
        return resultCounts.get(SHARE).success;
    }

    private List<Share> createShares(byte[] bytesToProtect, int recombinationThreshold, int numShares) {
        // todo actually do a secret share
        return IntStream.range(0, numShares).mapToObj(i -> new Share(bytesToProtect, this)).toList();
    }

    public void share(byte[] bytesToProtect, int recombinationThreshold, List<HelperClient> pairedHelpers) {
        if (Objects.isNull(bytesToProtect)) {
            throw new IllegalArgumentException("Bytes to protect is null");
        }
        if (Objects.nonNull(protectedValue)) {
            throw new IllegalStateException("A version must only be shared once");
        }
        this.protectedValue = bytesToProtect;
        this.recombinationThreshold = recombinationThreshold;
        this.shares = createShares(bytesToProtect, recombinationThreshold, pairedHelpers.size());
        Iterator<Share> shareIterator = this.shares.iterator();
        Iterator<HelperClient> helperIterator = pairedHelpers.iterator();
        while (shareIterator.hasNext()) {
            Version.Share share = shareIterator.next();
            share.helper = helperIterator.next();
            share.helper.send(share);
            resultCounts.get(SHARE).requestsSent++;
        }

        verificationTimer = scheduledExecutorService.scheduleAtFixedRate(
                this::verify,
                // TODO get the configured reverification stuff
                Util.RetryParameters.DEFAULT.reverification.getSeconds(),
                Util.RetryParameters.DEFAULT.reverification.getSeconds(), TimeUnit.SECONDS);
    }

    synchronized public void verify() {
        logger.info("Starting verification {}/{}", this.secret.secretId, versionNumber);
        resultCounts.put(VERIFY, new ResultCount());
        for (Share share : shares) {
            if (share.isShared) {
                // todo need unique id for this verification, we can use the nonce
                // of the request/reply to do this
                resultCounts.get(VERIFY).requestsSent++;
                share.helper.verify(share);
            }
        }
    }

    synchronized private void processResult(Result latestUpdate) {
        notifyStatus(latestUpdate.resultType.equals(SHARE) ? UPDATE_PROGRESS : VERIFY_PROGRESS,
                latestUpdate.share.helper, latestUpdate.message);

        ResultCount resultCounter = resultCounts.get(latestUpdate.resultType);

            if (latestUpdate.success) {
                resultCounter.successfulRepliesReceived++;
                if (resultCounter.successfulRepliesReceived >= recombinationThreshold) {
                    // if it hasn't already been set as successful
                    if (!resultCounter.success) {
                        resultCounter.success = true;
                        if (latestUpdate.resultType.equals(SHARE)) {
                            future.complete(this);
                        }
                        notifyStatus(latestUpdate.resultType.equals(SHARE) ? UPDATE_AVAILABLE : VERIFY_AVAILABLE);
                    }
                }
            } else {
                resultCounter.failedRepliesReceived++;
                // note that this is only paired helpers not all invited helpers
                if (resultCounter.requestsSent - resultCounter.failedRepliesReceived < recombinationThreshold) {
                    if (!resultCounter.reported) {
                        if (latestUpdate.resultType.equals(SHARE)) {
                            future.complete(this);
                        }
                        resultCounter.reported = true;
                        notifyStatus(latestUpdate.resultType.equals(SHARE) ? UPDATE_FAILED : VERIFY_FAILED);
                    }
                }
                // we'd want to do a retry or mark a helper as failed if we were doing that kind of thing
            }
            if (resultCounter.successfulRepliesReceived + resultCounter.failedRepliesReceived == resultCounter.requestsSent) {
                notifyStatus(latestUpdate.resultType.equals(SHARE) ? UPDATE_COMPLETE : VERIFY_COMPLETE);
            }
    }

    public void close() {
        future.cancel(true);
        verificationTimer.cancel(true);
        for (Share share : shares) {
            share.close();
        }
        scheduledExecutorService.shutdownNow();
    }

    /**
     * A share of a secret for a helper
     */
    public static class Share {
        final Version version;
        public Result latestUpdate;
        CompletableFuture<Share> future;
        byte[] shareContent; // contents of the share
        HelperClient helper; // the helper who was sent this share
        boolean isShared; // the Share was shared successfully

        public Share(byte[] shareContent, Version version) {
            this.version = version;
            this.shareContent = shareContent;
        }

        public void processResult(ResultType updateType, boolean success, String message) {
            if (updateType.equals(SHARE)) {
                isShared = success;
            }
            this.latestUpdate = new Result(updateType, success, this, message);
            version.processResult(this.latestUpdate);
        }

        public void close() {
            future.cancel(true);
        }
    }

    public enum ResultType {SHARE, VERIFY}

    /**
     * The result of an update or verification
     * @param resultType
     * @param success
     * @param share
     * @param message
     * @param timestamp
     */
    public record Result(
            ResultType resultType,
            boolean success, // update or verify succeeded
            Share share,
            String message, // any additional data
            ZonedDateTime timestamp) {

        public Result(
                ResultType type,
                boolean success, // update or verify succeeded
                Share share,
                String message) {
            this(type, success, share, message, ZonedDateTime.now());
        }

        public Result(
                ResultType type,
                boolean success,
                Share share) {
            this(type, success, share, "");
        }
    }

    static class ResultCount {
        // have we reported the outcome
        public boolean reported;
        // was it successful
        boolean success;
        // number of requests sent (shares)
        int requestsSent;
        int successfulRepliesReceived;
        int failedRepliesReceived;
    }
}
