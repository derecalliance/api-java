package com.thebuildingblocks.derec.v0_9.httpprototype;

import com.thebuildingblocks.derec.v0_9.interfaces.DeRecPairable;
import com.thebuildingblocks.derec.v0_9.interfaces.DeRecStatusNotification;
import derec.message.Derecmessage.DeRecMessage.HelperMessageBody.BodyCase;
import derec.message.ResultOuterClass;
import derec.message.Storeshare;
import derec.message.Verify;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static com.thebuildingblocks.derec.v0_9.httpprototype.Version.ResultType.SHARE;
import static com.thebuildingblocks.derec.v0_9.httpprototype.Version.ResultType.VERIFY;
import static com.thebuildingblocks.derec.v0_9.interfaces.DeRecPairable.PairingStatus.*;
import static com.thebuildingblocks.derec.v0_9.interfaces.DeRecStatusNotification.Type.*;
import static derec.message.Derecmessage.DeRecMessage.HelperMessageBody.BodyCase.*;
import static derec.message.ResultOuterClass.StatusEnum.OK;

/**
 * Provides a binding between the world of Protobuf Messages and DeRec Classes for responses to client
 */
public class HelperClientResponseProcessing {

    /* -- pair and unpair processing -- */

    private final static Map<BodyCase, PairingResponseProcessingStatus> responsesStatuses = new HashMap<>();

    static {
        responsesStatuses.put(PAIRRESPONSEMESSAGE,
                new PairingResponseProcessingStatus(PAIRED, REFUSED, HELPER_READY, HELPER_NOT_PAIRED));
        responsesStatuses.put(UNPAIRRESPONSEMESSAGE,
                new PairingResponseProcessingStatus(REMOVED, FAILED, HELPER_UNPAIRED, HELPER_INACTIVE));
    }

    public static HelperClient pairProcessResponse(HttpResponse<InputStream> response, HelperClient client) {
        if (pairingProcessResponse(response, client, PAIRRESPONSEMESSAGE)) {
            // TODO: do things like figure out what the comms parameters are
        }
        return client;
    }

    public static HelperClient unPairProcessResponse(HttpResponse<InputStream> response, HelperClient client) {
        pairingProcessResponse(response, client, UNPAIRRESPONSEMESSAGE);
        return client;
    }

    private static boolean pairingProcessResponse(HttpResponse<InputStream> response,
                                                  HelperClient helperClient,
                                                  BodyCase bodyCase) {
        // get the right set of status and notification codes for this message type
        PairingResponseProcessingStatus processingStatus = responsesStatuses.get(bodyCase);
        // handy callable notification function
        BiConsumer<Boolean, String> reporter = (b, s) -> helperClient.secret.notifyStatus(Notification.newBuilder()
                .secret(helperClient.secret)
                .pairable(helperClient)
                .message(s)
                .build(b ? processingStatus.successNotification() : processingStatus.failNotification()));

        // HTTP response failure
        if (response.statusCode() != 200) {
            reporter.accept(false, "HTTP Status " + response.statusCode());
            helperClient.status = processingStatus.failStatus();
            return false;
        }
        // HTTP success
        try {
            HelperClientMessageDeserializer messageDeserializer =
                    HelperClientMessageDeserializer.newInstance(response, bodyCase);
            ResultOuterClass.Result result = messageDeserializer.getResult();
            // server failed
            if (!result.getStatus().equals(OK)) {
                reporter.accept(false, result.getStatus() + " " + result.getMemo());
                helperClient.status = processingStatus.failStatus();
                return false;
            }
            // server success
            reporter.accept(true, result.getStatus() + " " + result.getMemo());
            helperClient.status = processingStatus.successStatus();
            return true;
        } catch (Exception e) {
            reporter.accept(false, "Exception processing response: " + e.getMessage());
            helperClient.status = processingStatus.failStatus();
            return false;
        }
    }

    public static Version.Share verifyResponseHandler(HttpResponse<InputStream> httpResponse,
                                                      Version.Share share) {
        BiConsumer<Boolean, String> reporter = (b, s) -> share.processResult(VERIFY, b, s);
        try {
            HelperClientMessageDeserializer messageDeserializer =
                    HelperClientMessageDeserializer.newInstance(httpResponse, VERIFYSHARERESPONSEMESSAGE);
            ResultOuterClass.Result result = messageDeserializer.getResult();
            if (shareResponseHandler(httpResponse, share, result, reporter)) {
                Verify.VerifyShareResponseMessage message =
                        messageDeserializer.getBodyMessages().get(VERIFYSHARERESPONSEMESSAGE).getVerifyShareResponseMessage();
                if (!Arrays.equals(message.getNonce().toByteArray(), share.nonce)) {
                    reporter.accept(false, "Nonce is not equal");
                }
                // TODO check the hash
                reporter.accept(true, result.getStatus() + " " + result.getMemo());
            }
        } catch (IOException e) {
            reporter.accept(false, e.getMessage());
        }
        share.future.complete(share);
        return share;
    }

    /* -- Verify and Store processing -- */

    public static Version.Share storeShareResponseHandler(HttpResponse<InputStream> httpResponse,
                                                          Version.Share share) {
        BiConsumer<Boolean, String> reporter = (b, s) -> share.processResult(SHARE, b, s);
        try {
            HelperClientMessageDeserializer messageDeserializer =
                    HelperClientMessageDeserializer.newInstance(httpResponse, STORESHARERESPONSEMESSAGE);
            ResultOuterClass.Result result = messageDeserializer.getResult();
            // check message result
            if (shareResponseHandler(httpResponse, share, result, reporter)) {
                Storeshare.StoreShareResponseMessage message =
                        messageDeserializer.getBodyMessages().get(STORESHARERESPONSEMESSAGE).getStoreShareResponseMessage();
                if (message.getVersion() != share.version.versionNumber) {
                    reporter.accept(false, "Version number is incorrect");
                }
            }
            reporter.accept(true, result.getStatus() + " " + result.getMemo());
        } catch (IOException e) {
            reporter.accept(false, e.getMessage());
        }
        share.future.complete(share);
        return share;
    }

    private static boolean shareResponseHandler(HttpResponse<InputStream> httpResponse,
                                                Version.Share share,
                                                ResultOuterClass.Result result,
                                                BiConsumer<Boolean, String> reporter) {
        // check HTTP status
        if (httpResponse.statusCode() != 200) {
            reporter.accept(false, "HTTP Status " + httpResponse.statusCode());
            return false;
        }
        try {
            // failed, so notify
            if (!result.getStatus().equals(OK)) {
                reporter.accept(false, result.getStatus() + " " + result.getMemo());
                return false;
            }
            return true;
        } catch (Exception e) {
            reporter.accept(false, e.getCause().getMessage());
            return false;
        }
    }

    /**
     * Records the outcome status for the helper and for the notification message for pair and upnair success and
     * failure
     */
    private record PairingResponseProcessingStatus(DeRecPairable.PairingStatus successStatus,
                                                   DeRecPairable.PairingStatus failStatus,
                                                   DeRecStatusNotification.Type successNotification,
                                                   DeRecStatusNotification.Type failNotification) {
    }
}
