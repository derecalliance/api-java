package com.thebuildingblocks.derec.v0_9.httpprototype;

import com.thebuildingblocks.derec.v0_9.interfaces.DeRecId;
import com.thebuildingblocks.derec.v0_9.interfaces.DeRecPairable;
import com.thebuildingblocks.derec.v0_9.interfaces.DeRecStatusNotification;
import derec.message.Derecmessage.DeRecMessage.HelperMessageBody;
import derec.message.ResultOuterClass;
import derec.message.Storeshare;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.function.Function;

import static com.thebuildingblocks.derec.v0_9.httpprototype.HelperClientMessageFactory.*;
import static com.thebuildingblocks.derec.v0_9.httpprototype.ResponseProcessing.getResponseProcessingStatus;
import static com.thebuildingblocks.derec.v0_9.httpprototype.Version.ResultType.SHARE;
import static com.thebuildingblocks.derec.v0_9.httpprototype.Version.ResultType.VERIFY;
import static com.thebuildingblocks.derec.v0_9.interfaces.DeRecPairable.PairingStatus.*;
import static com.thebuildingblocks.derec.v0_9.interfaces.DeRecStatusNotification.Type.*;
import static derec.message.Derecmessage.DeRecMessage.HelperMessageBody.BodyCase.*;
import static derec.message.ResultOuterClass.StatusEnum.OK;
import static derec.message.Storeshare.*;

/**
 * Sharer's view of a helper for a single secret, there will be multiple entries for the
 * same helper - one for each secret shared to that helper
 */
public class HelperClient implements DeRecPairable, Closeable {
    private final Secret secret; // the secret this helper is a helper for
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
                .thenApply(r -> this.processResponse(r, PAIRRESPONSEMESSAGE))
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

        /*
         * In-line functions following serve to capture the share
         */
        //noinspection DuplicatedCode
        Function<HttpResponse<InputStream>, Version.Share> processSharResponseStatusFn = httpResponse -> {
            if (httpResponse.statusCode() != 200) {
                share.processResult(SHARE, false, "HTTP Status " + httpResponse.statusCode());
            }
            try {
                HelperServerMessageDeserializer messageDeserializer =
                        HelperServerMessageDeserializer.newInstance(httpResponse, STORESHARERESPONSEMESSAGE);
                ResultOuterClass.Result result = messageDeserializer.getResult();
                if (!result.getStatus().equals(OK)) {
                    share.processResult(SHARE, false, result.getStatus() + " " + result.getMemo());
                }
                StoreShareResponseMessage message = messageDeserializer.getBodyMessage(STORESHARERESPONSEMESSAGE, StoreShareResponseMessage.class);
                if (message.getVersion() != share.version.versionNumber) {
                    share.processResult(SHARE, false, "message version mismatch");
                }
                share.processResult(SHARE, true, result.getMemo());
            } catch (IOException e) {
                share.processResult(SHARE, false, e.getCause().getMessage());
            }
            share.future.complete(share);
            return share;
        };

        //noinspection DuplicatedCode
        Function<Throwable, Version.Share> shareRequestFailedHandler = throwable -> {
            share.processResult(SHARE, false, throwable.getCause().getMessage());
            share.future.complete(share);
            return share;
        };

        HttpRequest request = buildRequest()
                .POST(BodyPublishers.ofByteArray(getMessage(
                        getShareRequestMessageBody(share)).toByteArray()))
                .build();

        if (Objects.isNull(share.helper)) {
            throw new IllegalStateException("Share helper must not be null");
        }
        share.helper = this;
        if (Objects.nonNull(share.future)) {
            share.future.cancel(true);
        }
        share.future = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(processSharResponseStatusFn)
                .exceptionally(shareRequestFailedHandler);
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

        /*
         * In-line functions following serve to capture the share
         */
        //noinspection DuplicatedCode
        Function<HttpResponse<byte[]>, Version.Share> verifyResponseHandler = httpResponse -> {
            // todo process message
            share.processResult(VERIFY, httpResponse.statusCode() == 200, "HTTP Status " + httpResponse.statusCode());
            share.future.complete(share);
            return share;
        };

        //noinspection DuplicatedCode
        Function<Throwable, Version.Share> verifyFailedHandler = throwable -> {
            share.processResult(VERIFY, false, throwable.getCause().getMessage());
            share.future.complete(share);
            return share;
        };

        HttpRequest request = buildRequest()
                .POST(BodyPublishers.ofByteArray(getMessage(
                        getVerifyRequestMessageBody(share)).toByteArray()))
                .build();

        share.future = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(verifyResponseHandler)
                .exceptionally(verifyFailedHandler);
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
                .POST(BodyPublishers.ofByteArray(getMessage(
                        getUnPairRequestMessageBody(reason)).toByteArray()))
                .build();


        pairingFuture = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(r -> this.processResponse(r, UNPAIRRESPONSEMESSAGE))
                .exceptionally(t -> {
                    this.status = FAILED;
                    secret.notifyStatus(buildNotification()
                            .message(t.getCause().getMessage())
                            .build(HELPER_INACTIVE));
                    return this;
                });
    }

    private HelperClient processResponse(HttpResponse<InputStream> response, HelperMessageBody.BodyCase bodyCase) {
        ResponseProcessing.ResponseProcessingStatus processingStatus = getResponseProcessingStatus(bodyCase);
        this.status = response.statusCode() == 200 ? processingStatus.successStatus() : processingStatus.failStatus();
        DeRecStatusNotification.Type type = processingStatus.failNotification();
        String message = "HTTP Status " + response.statusCode();
        if (response.statusCode() == 200) {
            try {
                HelperServerMessageDeserializer messageDeserializer = HelperServerMessageDeserializer.newInstance(response, bodyCase);
                ResultOuterClass.Result result = messageDeserializer.getResult();
                message = result.getMemo();
                if (result.getStatus().equals(OK)) {
                    type = processingStatus.successNotification();
                }  else {
                    logger.error("Error {} {} {}", bodyCase, result.getStatus(), message);
                    this.status = processingStatus.failStatus();
                }
            } catch (Exception e) {
                logger.error("Exception reading response", e);
                message = "Exception reading response: " +  e.getMessage();
                this.status = processingStatus.failStatus();
            }
        }
        secret.notifyStatus(buildNotification()
                .message(message)
                .build(type));
        return this;
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
