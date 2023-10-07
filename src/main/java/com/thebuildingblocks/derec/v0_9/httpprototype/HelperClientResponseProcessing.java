package com.thebuildingblocks.derec.v0_9.httpprototype;

import com.thebuildingblocks.derec.v0_9.interfaces.DeRecPairable;
import com.thebuildingblocks.derec.v0_9.interfaces.DeRecStatusNotification;
import derec.message.Derecmessage;
import derec.message.ResultOuterClass;
import derec.message.Storeshare;
import derec.message.Verify;

import java.io.InputStream;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

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
    public record PairingResponseProcessingStatus(DeRecPairable.PairingStatus successStatus,
                                                  DeRecPairable.PairingStatus failStatus,
                                                  DeRecStatusNotification.Type successNotification,
                                                  DeRecStatusNotification.Type failNotification){}

    private final static Map<Derecmessage.DeRecMessage.HelperMessageBody.BodyCase, PairingResponseProcessingStatus> responsesStatuses = new HashMap<>();
    static PairingResponseProcessingStatus unPairStatus = new PairingResponseProcessingStatus(REMOVED, FAILED, HELPER_UNPAIRED, HELPER_INACTIVE);
    static PairingResponseProcessingStatus pairStatus = new PairingResponseProcessingStatus(PAIRED, REFUSED, HELPER_READY, HELPER_NOT_PAIRED);

    static {
        responsesStatuses.put(Derecmessage.DeRecMessage.HelperMessageBody.BodyCase.PAIRRESPONSEMESSAGE, pairStatus);
        responsesStatuses.put(Derecmessage.DeRecMessage.HelperMessageBody.BodyCase.UNPAIRRESPONSEMESSAGE, unPairStatus);
    }

    public static PairingResponseProcessingStatus getResponseProcessingStatus(Derecmessage.DeRecMessage.HelperMessageBody.BodyCase bodyCase) {
        return responsesStatuses.get(bodyCase);
    }

    static boolean checkVerifyResponse(Derecmessage.DeRecMessage.HelperMessageBody body, Version.Share share, BiConsumer<Boolean, String> reporter) {
        Verify.VerifyShareResponseMessage message = body.getVerifyShareResponseMessage();
        if (Arrays.equals(message.getNonce().toByteArray(), share.nonce)) {
            return true;
        }
        reporter.accept(false, "Nonce is not equal");
        return false;
    }

    static boolean checkStoreShareResponse(Derecmessage.DeRecMessage.HelperMessageBody body, Version.Share share, BiConsumer<Boolean, String> reporter) {
        Storeshare.StoreShareResponseMessage message = body.getStoreShareResponseMessage();
        if (message.getVersion() == share.version.versionNumber) {
            return true;
        }
        reporter.accept(false, "Nonce is not equal");
        return false;
    }

    public static Version.Share verifyResponseHandler(HttpResponse<InputStream> httpResponse,
                                                      Version.Share share){
        BiConsumer<Boolean, String> reporter = (b, s) -> share.processResult(VERIFY, b, s);
        return shareResponseHandler(httpResponse,
                share,
                VERIFYSHARERESPONSEMESSAGE,
                (m) -> HelperClientResponseProcessing.checkVerifyResponse(m, share, reporter),
                reporter);
    }
    public static Version.Share storeShareResponseHandler(HttpResponse<InputStream> httpResponse,
                                                      Version.Share share){
        BiConsumer<Boolean, String> reporter = (b, s) -> share.processResult(SHARE, b, s);
        return shareResponseHandler(httpResponse,
                share,
                STORESHARERESPONSEMESSAGE,
                (m) -> HelperClientResponseProcessing.checkStoreShareResponse(m, share, reporter),
                reporter);
    }

    private static Version.Share shareResponseHandler(HttpResponse<InputStream> httpResponse,
                                                      Version.Share share,
                                                      Derecmessage.DeRecMessage.HelperMessageBody.BodyCase bodyCase,
                                                      Function<Derecmessage.DeRecMessage.HelperMessageBody, Boolean> verifier,
                                                      BiConsumer<Boolean, String> reporter){
        // check HTTP status
        if (httpResponse.statusCode() != 200) {
            reporter.accept(false,"HTTP Status " + httpResponse.statusCode());
        } else {
            try {
                // deserialize as the intended candidate message type
                HelperServerMessageDeserializer messageDeserializer =
                        HelperServerMessageDeserializer.newInstance(httpResponse, bodyCase);
                // check message result
                ResultOuterClass.Result result = messageDeserializer.getResult();
                if (!result.getStatus().equals(OK)) {
                    reporter.accept(false, result.getStatus() + " " + result.getMemo());
                } else {
                    if (verifier.apply(messageDeserializer.getBodyMessages().get(bodyCase))){
                        reporter.accept(true, result.getMemo());
                    }
                }
            } catch (Exception e) {
                reporter.accept(false, e.getCause().getMessage());
            }
        }
        share.future.complete(share);
        return share;
    }

    public static HelperClient pairProcessResponse (HttpResponse<InputStream> response, HelperClient client){
        return pairingProcessResponse(response, client, PAIRRESPONSEMESSAGE);
    }

    public static HelperClient unPairProcessResponse (HttpResponse<InputStream> response, HelperClient client){
            return pairingProcessResponse(response, client, UNPAIRRESPONSEMESSAGE);
    }
    public static HelperClient pairingProcessResponse(HttpResponse<InputStream> response,
                                         HelperClient helperClient,
                                         Derecmessage.DeRecMessage.HelperMessageBody.BodyCase bodyCase) {

        HelperClientResponseProcessing.PairingResponseProcessingStatus processingStatus = getResponseProcessingStatus(bodyCase);
        helperClient.status = response.statusCode() == 200 ? processingStatus.successStatus() : processingStatus.failStatus();
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
                    helperClient.status = processingStatus.failStatus();
                }
            } catch (Exception e) {
                message = "Exception reading response: " +  e.getMessage();
                helperClient.status = processingStatus.failStatus();
            }
        }
        helperClient.secret.notifyStatus(Notification.newBuilder()
                .secret(helperClient.secret)
                .pairable(helperClient)
                .message(message)
                .build(type));
        return helperClient;
    }

}
