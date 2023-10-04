package com.thebuildingblocks.derec.v0_9.httpprototype;

import com.thebuildingblocks.derec.v0_9.interfaces.DeRecId;
import com.thebuildingblocks.derec.v0_9.interfaces.DeRecPairable;
import com.thebuildingblocks.derec.v0_9.interfaces.DeRecStatusNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import static com.thebuildingblocks.derec.v0_9.interfaces.DeRecPairable.PairingStatus.*;
import static com.thebuildingblocks.derec.v0_9.interfaces.DeRecStatusNotification.Type.*;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Sharer's view of a helper for a single secret, there will be multiple entries for the
 * same helper - one for each secret shared to that helper
 */
public class HelperClient implements DeRecPairable, Closeable {
    private final Secret secret; // the secret this helper is a helper for
    DeRecId helperId; // unique Id for helper
    URI tsAndCs;    // link to legal conditions regarding what the helper is to do about
    // authentication for recovery and substitution of sharer
    PublicKey publicKey; // public key for the helper (for this secret)
    X509Certificate certificate; // The helper's certificate
    String protocolVersion; // accepted protocol version
    PairingStatus status = PairingStatus.NONE; // pairing not yet attempted
    String message = "";

    // a list of the shares sent to this helper - this is basically
    // a filtered view of secret.versions for this helper
    NavigableMap<Integer, Version.Share> shares = Collections.synchronizedNavigableMap(new TreeMap<>());
    CompletableFuture<HelperClient> pairingFuture; // awaits completion of pairing or unpairing

    // convenience function to build requests consistently
    HttpRequest.Builder buildRequest (){
        return HttpRequest.newBuilder()
                .uri(helperId.getAddress())
                .timeout(secret.retryParameters.getResponseTimeout());
    }

    // convenience function to avoid duplication
    Notification.Builder buildNotification() {
        return Notification.newBuilder()
                .secret(secret)
                .pairable(this);
    }

    Logger logger = LoggerFactory.getLogger(this.getClass());

    HelperClient(Secret secret, DeRecId helperId) {
        this.secret = secret;
        this.helperId = helperId;
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
                .POST(BodyPublishers.ofString("Pair Request: " + secret.sharerId.getName(), UTF_8))
                .build();

        pairingFuture = secret.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(this::processPairingResponseStatus)
                .thenApply(HttpResponse::body)
                .thenApply(this::processPairingResponseBody)
                .exceptionally(t -> {
                    this.status = FAILED;
                    secret.notifyStatus(buildNotification()
                            .message(t.getCause().getMessage())
                            .build(HELPER_NOT_PAIRED));
                    return this;
                });
    }

    private HttpResponse<byte[]> processPairingResponseStatus(HttpResponse<byte[]> response) {
        this.status = response.statusCode() == 200 ? PAIRED : REFUSED;
        this.message = "HTTP Status " + response.statusCode();
        logger.trace("Status {} Body {}", response.statusCode(), new String(response.body(), UTF_8));
        secret.notifyStatus(buildNotification()
                .message(this.message)
                .build(this.status == PAIRED ? HELPER_READY : HELPER_NOT_PAIRED));
        return response;
    }

    private HelperClient processPairingResponseBody(byte[] bytes) {
        // todo: process the returned message

        return this;
    }

    public void send(Version.Share share) {
        synchronized (this) {
            if (!status.equals(PAIRED)) {
                throw new IllegalStateException("Helper must be paired to share");
            }
            shares.put(share.version.versionNumber, share);
        }
        // todo: check already allocated
        share.helper = this;

        /*
         * In-line functions following serve to capture the share
         */
        Function<HttpResponse<byte[]>, HttpResponse<byte[]>> processSharResponseStatusFn = httpResponse ->
                processShareResponseStatus(share, httpResponse);

        Function<byte[], Version.Share> processShareResponseBody = bytes -> {
            // todo: do something with the ShareResponse message
            return share;
        };

        Function<Throwable, Version.Share> shareRequestFailedHandler = throwable -> {
            // todo: do something with exception
            // todo move code to share
            synchronized (share.version) {
                share.version.failedUpdateReplyReceived++;
            }
            return share;
        };

        HttpRequest request = buildRequest()
                .POST(BodyPublishers.ofByteArray(share.shareContent))
                .build();

        share.future = share.version.secret.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(processSharResponseStatusFn)
                .thenApply(HttpResponse::body)
                .thenApply(processShareResponseBody)
                .exceptionally(shareRequestFailedHandler);

        // todo: move code and synchronize
        share.version.updateRequestSent++;
    }

    HttpResponse<byte[]> processShareResponseStatus(Version.Share share, HttpResponse<byte[]> httpResponse) {
        Version version = share.version;
        //todo move code to share
        synchronized (version) {
            if (httpResponse.statusCode() == 200) {
                version.successfulUpdateRepliesReceived++;
                logger.trace("Version {} Success from {} helpers", version.versionNumber,
                        version.successfulUpdateRepliesReceived);
                if (version.successfulUpdateRepliesReceived >= secret.thresholdForDeletion) {
                    // if it hasn't already been set as successful
                    if (!version.success) {
                        share.confirmed = ZonedDateTime.now();
                        version.success = true;
                        version.future.complete(version);
                        secret.notifyStatus(buildNotification().
                                version(version).
                                build(UPDATE_AVAILABLE));
                    }
                }
            } else {
                version.failedUpdateReplyReceived++;
                // too many failed??
                if (version.shares.size() - version.failedUpdateReplyReceived < secret.thresholdForDeletion) {
                    version.future.complete(version);
                }
            }
            if (version.successfulUpdateRepliesReceived + version.failedUpdateReplyReceived == version.shares.size()) {
                secret.notifyStatus(buildNotification()
                        .message(version.success ? "Update succeeded" : "Update failed")
                        .version(version)
                        .build(UPDATE_FINISHED));
            }
        }
        return httpResponse;
    }

    /**
     * Remove pairing with this helper
     */
    public void unPair() {
        synchronized (this) {
            if (!status.equals(PAIRED)) {
                // todo need to cancel an in progress pairing
                throw new IllegalStateException("Cannot unpair an unpaired helper");
            }
            status = PairingStatus.PENDING_REMOVAL;
        }

        HttpRequest request = buildRequest()
                .POST(BodyPublishers.ofByteArray(("UnPair Request: " + secret.sharerId.getName()).getBytes(UTF_8)))
                .build();


        pairingFuture = secret.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(this::processUnPairingResponseStatus)
                .thenApply(HttpResponse::body)
                .thenApply(this::processUnPairingResponseBody)
                .exceptionally(t -> {
                    this.status = FAILED;
                    return this;
                });
    }

    private HttpResponse<byte[]> processUnPairingResponseStatus(HttpResponse<byte[]> response) {
        this.status = response.statusCode() == 200 ? PairingStatus.REMOVED : FAILED;
        this.message = "HTTP Status " + response.statusCode();
        return response;
    }

    private HelperClient processUnPairingResponseBody(byte[] bytes) {
        // todo: process the returned message
        secret.notifyStatus(buildNotification()
                .message(this.helperId.getName())
                .build(HELPER_INACTIVE));
        return this;
    }

    public void close() {
        if (status.equals(PAIRED)) {
            unPair();
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
