package com.thebuildingblocks.derec.v1.skeleton;

import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Sharer's view of a helper for a single secret, there will be multiple entries for the
 * same helper - one for each secret that have shared to that helper
 */
public class HelperClient implements Closeable {
    public enum Status {
        NONE, // not yet invited
        INVITED, // no reply yet
        PAIRED, // replied positively
        REFUSED, // replied negatively
        PENDING_REMOVAL, // in the process of being removed
        REMOVED,
        GONE // timeout, disconnect etc.
    }

    private final Secret secret; // the secret this helper is a helper for
    DeRecId helperId; // unique Id for helper
    URI tsAndCs;    // link to legal conditions regarding what the helper is to do about
    // authentication for recovery and substitution of sharer
    PublicKey publicKey; // public key for the helper (for this secret)
    X509Certificate certificate; // The helper's certificate
    String protocolVersion; // accepted protocol version

    Status status = Status.NONE; // pairing not yet attempted
    Util.RetryStatus retryStatus; // some kind of way of figuring out retry and timeout,
    // todo: retry status actually needs to be part of the Share

    /* -- todo: these need to be thread safe --*/
    int lastConfirmedShareVersion; // which version has the helper got?
    Map<Integer, Secret.Share> shares; // a list of the shares sent to this helper - this is basically
    // a filtered view of secret.versions for this helper

    CompletableFuture<HelperClient> pairingFuture; // awaits completion of pairing
    private final OkHttpClient client = new OkHttpClient(); // one client per helper not actually needed

    Logger logger = LoggerFactory.getLogger(this.getClass());

    HelperClient(Secret secret, DeRecId helperId) {
        retryStatus = new Util.RetryStatus();
        shares = new HashMap<>();
        this.secret = secret;
        this.helperId = helperId;
    }

    /**
     * Initiate pairing with this helper
     */
    public void pair() {
        if (status.equals(Status.PAIRED) || status.equals(Status.INVITED)) {
            throw new IllegalStateException("Cannot pair a paired helper");
        }
        status = Status.INVITED;

        Request request = null;
        try {
            request = new Request.Builder()
                    .url(helperId.address.toURL())
                    .post(RequestBody.create(("Pair Request: " + secret.sharerId.name).getBytes(StandardCharsets.UTF_8)))
                    .build();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }


        Call call = client.newCall(request);
        pairingFuture = new CompletableFuture<>();
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                handleFail();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        status = Status.PAIRED;
                        pairingFuture.complete(HelperClient.this);
                        return;
                    }
                }
                handleFail();
            }

            private void handleFail() {
                status = Status.REFUSED;
                pairingFuture.complete(HelperClient.this);
            }
        });
    }

    public void send(Secret.Version version, int i) {
        Secret.Share share = version.shares.get(i);
        share.helper = this.helperId;
        shares.put(version.versionNumber, version.shares.get(i));
        Request request = null;
        try {
            request = new Request.Builder()
                    .url(helperId.address.toURL())
                    .post(RequestBody.create(share.shareContent))
                    .build();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }


        Call call = client.newCall(request);
        version.future = new CompletableFuture<>();
        call.enqueue(new Callback() {
            final DeRecId helperId = HelperClient.this.helperId;

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                handleFail();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                try (ResponseBody responseBody = response.body()) {
                    logger.info("Response from {} - success: {}", helperId.name, response.isSuccessful());
                    if (response.isSuccessful()) {
                        version.successfulUpdateRepliesReceived++;
                        if (version.successfulUpdateRepliesReceived >= secret.thresholdForDeletion) {
                            // if it hasn't already been set as successful
                            if (!version.success) {
                                lastConfirmedShareVersion = version.versionNumber;
                                shares.get(version.versionNumber).confirmed = ZonedDateTime.now();
                                version.success = true;
                                version.future.complete(version);
                                logger.info("Success from {} helpers", secret.thresholdForDeletion);
                            }
                        }
                        return;
                    }
                }
                handleFail();
            }

            private void handleFail() {
                version.failedUpdateReplyReceived++;
                if (version.successfulUpdateRepliesReceived + version.failedUpdateReplyReceived == version.shares.size()) {
                    if (!version.future.isDone()) {
                        version.future.complete(version);
                    }
                }
            }
        });
        version.updateRequestSent++;
    }

    /**
     * Remove pairing with this helper
     */
    public void unPair() {
        if (!status.equals(Status.PAIRED)) {
            // todo need to cancel an in progress pairing
            throw new IllegalStateException("Cannot unpair an unpaired helper");
        }
        status = Status.PENDING_REMOVAL;

        Request request = null;
        try {
            request = new Request.Builder()
                    .url(helperId.address.toURL())
                    .post(RequestBody.create(("UnPair Request: " + secret.sharerId.name).getBytes(StandardCharsets.UTF_8)))
                    .build();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }


        Call call = client.newCall(request);
        pairingFuture = new CompletableFuture<>();
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                handleFail();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        status = Status.REMOVED;
                        pairingFuture.complete(HelperClient.this);
                        return;
                    }
                }
                handleFail();
            }

            private void handleFail() {
                status = Status.REMOVED;
                pairingFuture.complete(HelperClient.this);
            }
        });
    }


    public void close() {
        if (status.equals(Status.PAIRED)) {
            unPair();
        }
        try {
            pairingFuture.get(1, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("Error unpairing from {}", helperId.name, e);
        }
        client.dispatcher().executorService().shutdownNow();
        client.connectionPool().evictAll();
    }
}
