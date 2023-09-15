package com.thebuildingblocks.derec.v1.skeleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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

/**
 * Sharer's view of a helper for a single secret, there will be multiple entries for the
 * same helper - one for each secret shared to that helper
 */
public class HelperClient implements Closeable {
    private final Secret secret; // the secret this helper is a helper for
    DeRecId helperId; // unique Id for helper
    URI tsAndCs;    // link to legal conditions regarding what the helper is to do about
    // authentication for recovery and substitution of sharer
    PublicKey publicKey; // public key for the helper (for this secret)
    X509Certificate certificate; // The helper's certificate
    String protocolVersion; // accepted protocol version
    Status status = Status.NONE; // pairing not yet attempted

    // a list of the shares sent to this helper - this is basically
    // a filtered view of secret.versions for this helper
    NavigableMap<Integer, Secret.Share> shares = Collections.synchronizedNavigableMap(new TreeMap<>());
    CompletableFuture<HelperClient> pairingFuture; // awaits completion of pairing or unpairing
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
            if (!status.equals(Status.NONE) && !status.equals(Status.FAILED)) {
                throw new IllegalStateException(String.format("Cannot pair a helper with status %s", status));
            }
            status = Status.INVITED;
        }

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(helperId.address)
                .POST(BodyPublishers.ofString("Pair Request: " + secret.sharerId.name, StandardCharsets.UTF_8))
                .build();


        pairingFuture = secret.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(this::processPairingResponseStatus)
                .thenApply(HttpResponse::body)
                .thenApply(this::processPairingResponseBody)
                .exceptionally(t -> {
                    this.status = Status.FAILED;
                    return this;
                });
    }

    private HttpResponse<byte[]> processPairingResponseStatus(HttpResponse<byte[]> response) {
        this.status = response.statusCode() == 200 ? Status.PAIRED : Status.REFUSED;
        return response;
    }

    private HelperClient processPairingResponseBody(byte[] bytes) {
        // todo: process the returned message
        return this;
    }

    public void send(Secret.Share share) {
        synchronized (this) {
            if (!status.equals(Status.PAIRED)) {
                throw new IllegalStateException("Helper must be paired to share");
            }
            shares.put(share.version.versionNumber, share);
        }
        // todo: check already allocated
        share.helper = this;

        /*
         * In-line functions following serve only to capture the share
         */
        Function<HttpResponse<byte[]>, HttpResponse<byte[]>> processSharResponseStatusFn = httpResponse ->
                processShareResponseStatus(share, httpResponse);

        Function<byte[], Secret.Share> processShareResponseBody = bytes -> {
            // todo: do something with the ShareResponse message
            return share;
        };

        Function<Throwable, Secret.Share> shareRequestFailedHandler = throwable -> {
            // todo: do something with exception
            // todo move code to share
            synchronized (share.version) {
                share.version.failedUpdateReplyReceived++;
            }
            return share;
        };

        HttpRequest request = HttpRequest.newBuilder()
                .uri(helperId.address)
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

    HttpResponse<byte[]> processShareResponseStatus(Secret.Share share, HttpResponse<byte[]> httpResponse) {
        Secret.Version version = share.version;
        //todo move code to share
        synchronized (version) {
            if (httpResponse.statusCode() == 200) {
                version.successfulUpdateRepliesReceived++;
                logger.info("Version {} Success from {} helpers", version.versionNumber,
                        version.successfulUpdateRepliesReceived);
                if (version.successfulUpdateRepliesReceived >= secret.thresholdForDeletion) {
                    // if it hasn't already been set as successful
                    if (!version.success) {
                        share.confirmed = ZonedDateTime.now();
                        version.success = true;
                        version.future.complete(version);
                    }
                }
            } else {
                version.failedUpdateReplyReceived++;
                // too many failed??
                if (version.shares.size() - version.failedUpdateReplyReceived < secret.thresholdForDeletion) {
                    version.future.complete(version);
                }
            }
        }
        return httpResponse;
    }

    /**
     * Remove pairing with this helper
     */
    public void unPair() {
        synchronized (this) {
            if (!status.equals(Status.PAIRED)) {
                // todo need to cancel an in progress pairing
                throw new IllegalStateException("Cannot unpair an unpaired helper");
            }
            status = Status.PENDING_REMOVAL;
        }

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(helperId.address)
                .POST(BodyPublishers.ofByteArray(("UnPair Request: " + secret.sharerId.name).getBytes(StandardCharsets.UTF_8)))
                .build();


        pairingFuture = secret.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(this::processUnPairingResponseStatus)
                .thenApply(HttpResponse::body)
                .thenApply(this::processUnPairingResponseBody)
                .exceptionally(t -> {
                    this.status = Status.FAILED;
                    return this;
                });
    }

    private HttpResponse<byte[]> processUnPairingResponseStatus(HttpResponse<byte[]> response) {
        this.status = response.statusCode() == 200 ? Status.REMOVED : Status.FAILED;
        return response;
    }

    private HelperClient processUnPairingResponseBody(byte[] bytes) {
        // todo: process the returned message
        return this;
    }

    public void close() {
        if (status.equals(Status.PAIRED)) {
            unPair();
        }
        try {
            // todo: actual timeout
            pairingFuture.get(1, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("Error unpairing from {}", helperId.name, e);
        }
    }

    public enum Status {
        NONE, // not yet invited
        INVITED, // no reply yet
        PAIRED, // replied positively
        REFUSED, // replied negatively
        PENDING_REMOVAL, // in the process of being removed
        REMOVED,
        FAILED, GONE // timeout, disconnect etc.
    }
}
