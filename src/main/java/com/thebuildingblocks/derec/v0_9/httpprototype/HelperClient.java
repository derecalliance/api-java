package com.thebuildingblocks.derec.v0_9.httpprototype;

import com.thebuildingblocks.derec.v0_9.interfaces.DeRecId;
import com.thebuildingblocks.derec.v0_9.interfaces.DeRecPairable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.thebuildingblocks.derec.v0_9.httpprototype.HelperClientMessageFactory.*;
import static com.thebuildingblocks.derec.v0_9.httpprototype.HelperClientResponseProcessing.*;
import static com.thebuildingblocks.derec.v0_9.httpprototype.Version.ResultType.SHARE;
import static com.thebuildingblocks.derec.v0_9.httpprototype.Version.ResultType.VERIFY;
import static com.thebuildingblocks.derec.v0_9.interfaces.DeRecPairable.PairingStatus.*;
import static com.thebuildingblocks.derec.v0_9.interfaces.DeRecStatusNotification.Type.*;

/**
 * Sharer's view of a helper for a single secret, there will be multiple entries for the
 * same helper - one for each secret shared to that helper
 */
public class HelperClient implements DeRecPairable, Closeable {
    public final Secret secret; // the secret this helper is a helper for
    private final Util.RetryParameters retryParameters;
    private final HttpClient httpClient;
    private final DeRecId helperId; // unique Id for helper
    URI tsAndCs;    // link to legal conditions regarding what the helper is to do about
    // authentication for recovery and substitution of sharer
    PublicKey publicKey; // public key for the helper (for this secret)
    X509Certificate certificate; // The helper's certificate
    String protocolVersion; // accepted protocol version
    PairingStatus status = PairingStatus.NONE; // pairing not yet attempted

    // a list of the shares sent to this helper - this is basically
    // a filtered view of secret.versions for this helper
    NavigableMap<Integer, Version.Share> shares = Collections.synchronizedNavigableMap(new TreeMap<>());
    CompletableFuture<HelperClient> pairingFuture; // awaits completion of pairing or unpairing
    Logger logger = LoggerFactory.getLogger(this.getClass());

    HelperClient(Secret secret, DeRecId helperId, HttpClient httpClient, Util.RetryParameters retryParameters) {
        this.secret = secret;
        this.helperId = helperId;
        this.httpClient = httpClient;
        this.retryParameters = retryParameters;
    }

    // convenience function to build requests consistently
    HttpRequest.Builder buildRequest() {
        return HttpRequest.newBuilder()
                .uri(helperId.getAddress())
                .timeout(retryParameters.getResponseTimeout());
    }

    // convenience function to avoid duplication
    Notification.Builder buildNotification() {
        return Notification.newBuilder()
                .secret(secret)
                .pairable(this);
    }

    /**
     * Initiate pairing with this helper
     */
    public void pair() {
        synchronized (this) {
            if (!status.equals(PairingStatus.NONE) && !status.equals(FAILED)) {
                throw new IllegalStateException(String.format("Cannot pair a helper with status %s", status));
            }
            status = PairingStatus.INVITED;
        }

        HttpRequest request = buildRequest()
                .POST(BodyPublishers.ofByteArray(getMessage(
                        getPairRequestMessageBody(helperId)).toByteArray()))
                .build();

        pairingFuture = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(r -> pairProcessResponse(r, this))
                .exceptionally(t -> {
                    this.status = FAILED;
                    secret.notifyStatus(buildNotification()
                            .message(t.getCause().getMessage())
                            .build(HELPER_NOT_PAIRED));
                    return this;
                });
    }

    public void send(Version.Share share) {
        synchronized (this) {
            if (!status.equals(PAIRED)) {
                throw new IllegalStateException("Helper must be paired to share");
            }
            shares.put(share.version.versionNumber, share);
        }

        HttpRequest request = buildRequest()
                .POST(BodyPublishers.ofByteArray(getMessage(
                        getShareRequestMessageBody(share)).toByteArray()))
                .build();

        if (Objects.isNull(share.helper)) {
            throw new IllegalStateException("Share helper must not be null");
        }
        if (Objects.nonNull(share.future)) {
            share.future.cancel(true);
        }
        //noinspection DuplicatedCode
        share.future = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(httpResponse -> HelperClientResponseProcessing.storeShareResponseHandler(httpResponse, share))
                .exceptionally(throwable -> {
                    share.processResult(SHARE, false, throwable.getCause().getMessage());
                    share.future.complete(share);
                    return share;
                });
    }

    public void verify(Version.Share share) {
        synchronized (this) {
            if (!status.equals(PAIRED)) {
                throw new IllegalStateException("Helper must be paired to share");
            }
            if (!share.isShared) {
                throw new IllegalStateException("Share must have been shared to verify");
            }
            if (!shares.containsKey(share.version.versionNumber)) {
                throw new IllegalStateException("Share must have been shared to verify");
            }
        }

        HttpRequest request = buildRequest()
                .POST(BodyPublishers.ofByteArray(getMessage(getVerifyRequestMessageBody(share)).toByteArray()))
                .build();

        //noinspection DuplicatedCode
        share.future = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(httpResponse -> HelperClientResponseProcessing.verifyResponseHandler(httpResponse, share))
                .exceptionally(throwable -> {
                    share.processResult(VERIFY, false, throwable.getCause().getMessage());
                    share.future.complete(share);
                    return share;
                });
    }

    /**
     * Remove pairing with this helper
     */
    public void unPair(String reason) {
        synchronized (this) {
            if (!status.equals(PAIRED)) {
                // todo need to cancel an in progress pairing
                throw new IllegalStateException("Cannot unpair an unpaired helper");
            }
            status = PairingStatus.PENDING_REMOVAL;
        }

        HttpRequest request = buildRequest()
                .POST(BodyPublishers.ofByteArray(getMessage(getUnPairRequestMessageBody(reason)).toByteArray()))
                .build();


        pairingFuture = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(r -> unPairProcessResponse(r, this))
                .exceptionally(t -> {
                    this.status = FAILED;
                    secret.notifyStatus(buildNotification()
                            .message(t.getCause().getMessage())
                            .build(HELPER_INACTIVE));
                    return this;
                });
    }

    public void close() {
        if (status.equals(PAIRED)) {
            unPair("Helper Client is Closing");
        }
        try {
            pairingFuture.get(secret.retryParameters.pairingWaitSecs, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("Error unpairing from {}", helperId.getName(), e);
        }
    }

    @Override
    public DeRecId getId() {
        return helperId;
    }

    @Override
    public PairingStatus getStatus() {
        return status;
    }
}
