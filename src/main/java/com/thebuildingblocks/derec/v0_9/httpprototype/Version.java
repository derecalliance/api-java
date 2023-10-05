package com.thebuildingblocks.derec.v0_9.httpprototype;

import com.thebuildingblocks.derec.v0_9.interfaces.DeRecSecret;
import com.thebuildingblocks.derec.v0_9.interfaces.DeRecVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static com.thebuildingblocks.derec.v0_9.httpprototype.Version.ResultType.SHARE;
import static com.thebuildingblocks.derec.v0_9.interfaces.DeRecStatusNotification.Type.*;


/**
 * Representing a version of a secret
 */
public class Version implements DeRecVersion {
    // the secret this version belongs to
    final Secret secret;
    // version numbers to be allocated so that they are always larger than the last shared
    final int versionNumber;
    public ScheduledFuture<?> verificationFuture;
    // the bytes of this secret
    byte[] protectedValue;
    // share threshold
    int threshold;
    // the shares created for this version
    List<Share> shares;
    // a future to complete when the sharing is complete successfully or is known to have failed
    CompletableFuture<Version> future = new CompletableFuture<>();
    ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);
    // whether it succeeded or failed
    boolean success;
    // count of update requests sent
    int updateRequestSent;
    // count of update replies received
    int successfulUpdateRepliesReceived;
    int failedUpdateReplyReceived;

    Logger logger = LoggerFactory.getLogger(this.getClass());

    Version(Secret secret, int versionNumber) {
        this.versionNumber = versionNumber;
        this.secret = secret;
    }

    private List<Share> createShares(byte[] bytesToProtect, int numShares) {
        return IntStream.range(0, numShares).mapToObj(i -> new Share(bytesToProtect, this)).toList();
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

    @Override
    public boolean isProtected() {
        return success;
    }

    public void verify() {
        logger.info("Starting verification {}/{}", this.secret.secretId, versionNumber);
        for (Share share : shares) {
            if (share.isShared) {
                share.helper.verify(share);
            }
        }
    }

    public void share(byte[] bytesToProtect, List<HelperClient> pairedHelpers) {
        if (Objects.isNull(bytesToProtect)) {
            throw new IllegalArgumentException("Bytes to protect is null");
        }
        if (Objects.nonNull(protectedValue)) {
            throw new IllegalStateException("A version may only be shared once");
        }
        this.protectedValue = bytesToProtect;
        this.shares = createShares(bytesToProtect, pairedHelpers.size());
        Iterator<Share> shareIterator = this.shares.iterator();
        Iterator<HelperClient> helperIterator = pairedHelpers.iterator();
        while (shareIterator.hasNext()) {
            Version.Share share = shareIterator.next();
            share.helper = helperIterator.next();
            share.helper.send(share);
            updateRequestSent++;
        }

        verificationFuture = scheduledExecutorService.scheduleAtFixedRate(
                this::verify,
                // TODO get the configured reverification stuff
                Util.RetryParameters.DEFAULT.reverification.getSeconds(),
                Util.RetryParameters.DEFAULT.reverification.getSeconds(), TimeUnit.SECONDS);
    }

    synchronized private void processResult(Result latestUpdate) {
        secret.notifyStatus(Notification.newBuilder()
                .secret(secret)
                .version(this)
                .pairable(latestUpdate.share.helper)
                .message(latestUpdate.message)
                .build(latestUpdate.resultType.equals(SHARE) ? UPDATE_PROGRESS : VERIFY_PROGRESS));

        if (latestUpdate.resultType.equals(SHARE)) {
            if (latestUpdate.success) {
                successfulUpdateRepliesReceived++;
                if (successfulUpdateRepliesReceived >= secret.thresholdSecretRecovery) {
                    // if it hasn't already been set as successful
                    if (!success) {
                        success = true;
                        future.complete(this);
                        secret.notifyStatus(Notification.newBuilder()
                                .secret(secret)
                                .version(this)
                                .build(UPDATE_AVAILABLE));
                    }
                }
            } else {
                failedUpdateReplyReceived++;
                // note that this is only paired helpers not all invited helpers
                if (shares.size() - failedUpdateReplyReceived < secret.thresholdForDeletion) {
                    future.complete(this);
                }
                // we'd want to do a retry or mark a helper as failed if we were doing that kind of thing
            }
        }
        if (successfulUpdateRepliesReceived + failedUpdateReplyReceived == updateRequestSent) {
            secret.notifyStatus(Notification.newBuilder()
                    .secret(secret)
                    .version(this)
                    .build(latestUpdate.resultType.equals(SHARE) ? UPDATE_COMPLETE : VERIFY_COMPLETE));
        }
    }

    public void close() {
        future.cancel(true);
        verificationFuture.cancel(true);
        for (Share share : shares) {
            share.close();
        }
        scheduledExecutorService.shutdownNow();
    }


    public enum ResultType {SHARE, VERIFY}

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
}
