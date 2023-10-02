package com.thebuildingblocks.derec.v0_9.httpprototype;

import com.thebuildingblocks.derec.v0_9.interfaces.DeRecSecret;
import com.thebuildingblocks.derec.v0_9.interfaces.DeRecVersion;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;


/**
 * Representing a version of a share
 */
public class Version implements DeRecVersion {
    // copy of the secret when the version was created
    final byte[] bytesToProtect;
    // the secret this version belongs to
    final Secret secret;
    // version numbers to be allocated so that they are always larger than the last shared
    final int versionNumber;
    // share threshold
    int threshold;
    // the shares created for this version
    List<Share> shares;
    // a future to complete when the sharing is complete successfully or is known to have failed
    CompletableFuture<Version> future = new CompletableFuture<>();
    // whether it succeeded or failed
    boolean success;
    // count of update requests sent
    int updateRequestSent;
    // count of update replies received
    int successfulUpdateRepliesReceived;
    int failedUpdateReplyReceived;

    Version(Secret secret, byte[] bytesToProtect, int latestShareVersion, int numShares) {
        this.versionNumber = latestShareVersion;
        this.secret = secret;
        this.bytesToProtect = bytesToProtect;
        this.shares = createShares(bytesToProtect, numShares);
    }

    List<Share> createShares(byte[] bytesToProtect, int numShares) {
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
        return bytesToProtect;
    }

    @Override
    public boolean isProtected() {
        return success;
    }


    /**
     * A share of a secret for a helper
     */
    public static class Share {
        final Version version;
        public CompletableFuture<Share> future;
        byte[] shareContent; // contents of the share
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
