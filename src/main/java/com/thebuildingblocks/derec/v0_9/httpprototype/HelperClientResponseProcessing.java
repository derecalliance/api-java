/*
 * Copyright (c) 2023 The Building Blocks Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thebuildingblocks.derec.v0_9.httpprototype;

import com.thebuildingblocks.derec.v0_9.interfaces.DeRecHelperStatus;
import com.thebuildingblocks.derec.v0_9.interfaces.DeRecStatusNotification;
import derec.message.*;
import derec.message.Derecmessage.DeRecMessage.HelperMessageBody.BodyCase;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.thebuildingblocks.derec.v0_9.httpprototype.Version.ResultType.SHARE;
import static com.thebuildingblocks.derec.v0_9.httpprototype.Version.ResultType.VERIFY;
import static com.thebuildingblocks.derec.v0_9.interfaces.DeRecHelperStatus.PairingStatus.*;
import static com.thebuildingblocks.derec.v0_9.interfaces.DeRecStatusNotification.StandardNotificationType.*;
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
                new PairingResponseProcessingStatus(REMOVED, FAILED, HELPER_UNPAIRED, HELPER_UNPAIRED));
    }

    public static HelperClient pairProcessResponse(InputStream inputStream, HelperClient helperClient) {
        // get the right set of status and notification codes for this message type
        PairingResponseProcessingStatus processingStatus = responsesStatuses.get(PAIRRESPONSEMESSAGE);
        // handy callable notification function
        BiConsumer<Boolean, String> reporter = (b, s) -> helperClient.secret.notifyStatus(Notification.newBuilder()
                .secret(helperClient.secret)
                .helper(helperClient)
                .message(s)
                .build(b ? processingStatus.successNotification() : processingStatus.failNotification()));
        HelperClientMessageDeserializer messageDeserializer =
                HelperClientMessageDeserializer.newInstance(inputStream, PAIRRESPONSEMESSAGE);
        Pair.PairResponseMessage message = messageDeserializer.getBody().getPairResponseMessage();
        // server failed
        if (!message.getResult().getStatus().equals(OK)) {
            reporter.accept(false, message.getResult().getStatus() + " " + message.getResult().getMemo());
            helperClient.status = processingStatus.failStatus();
            return helperClient;
        }
        // TODO: do things like figure out what the comms parameters are
        reporter.accept(true, message.getResult().getStatus() + " " + message.getResult().getMemo());
        helperClient.status = processingStatus.successStatus();
        return helperClient;
    }

    public static HelperClient unPairProcessResponse(InputStream inputStream, HelperClient helperClient) {
        // get the right set of status and notification codes for this message type
        PairingResponseProcessingStatus processingStatus = responsesStatuses.get(UNPAIRRESPONSEMESSAGE);
        HelperClientMessageDeserializer messageDeserializer = HelperClientMessageDeserializer.newInstance(inputStream, UNPAIRRESPONSEMESSAGE);
        Unpair.UnpairResponseMessage message = messageDeserializer.getBody().getUnpairResponseMessage();
        Consumer<DeRecStatusNotification.StandardNotificationType> reporter = (t) -> helperClient.secret.notifyStatus(Notification.newBuilder()
                .secret(helperClient.secret)
                .helper(helperClient)
                .message(message.getResult().getStatus() + " " + message.getResult().getMemo())
                .build(t));
        // server failed
        if (!message.getResult().getStatus().equals(OK)) {
            reporter.accept(processingStatus.failNotification());
            helperClient.status = processingStatus.failStatus();
            return helperClient;
        }
        reporter.accept(processingStatus.successNotification());
        helperClient.status = processingStatus.successStatus();
        return helperClient;
    }

    private static boolean pairingProcessResponse(BodyCase bodyCase,
                                                  ResultOuterClass.Result result,
                                                  HelperClient helperClient) {
        // get the right set of status and notification codes for this message type
        PairingResponseProcessingStatus processingStatus = responsesStatuses.get(bodyCase);
        // handy callable notification function
        BiConsumer<Boolean, String> reporter = (b, s) -> helperClient.secret.notifyStatus(Notification.newBuilder()
                .secret(helperClient.secret)
                .helper(helperClient)
                .message(s)
                .build(b ? processingStatus.successNotification() : processingStatus.failNotification()));

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
    }

    public static Version.Share verifyResponseHandler(InputStream inputStream, Version.Share share) {
        BiConsumer<Boolean, String> reporter = (b, s) -> share.processResult(VERIFY, b, s);
        // check there is an appropriate messageBody
        HelperClientMessageDeserializer messageDeserializer =
                HelperClientMessageDeserializer.newInstance(inputStream, VERIFYSHARERESPONSEMESSAGE);
        Verify.VerifyShareResponseMessage message = messageDeserializer.getBody().getVerifyShareResponseMessage();
        ResultOuterClass.Result result = message.getResult();
        if (!result.getStatus().equals(OK)) {
            reporter.accept(false, result.getStatus() + " " + result.getMemo());
            return share;
        }
        if (!Arrays.equals(message.getNonce().toByteArray(), share.nonce)) {
            reporter.accept(false, "Nonce is not equal");
            return share;
        }
        // TODO check the hash
        reporter.accept(true, result.getStatus() + " " + result.getMemo());
        return share;
    }

    /* -- Verify and Store processing -- */

    public static Version.Share storeShareResponseHandler(InputStream inputStream, Version.Share share) {
        BiConsumer<Boolean, String> reporter = (b, s) -> share.processResult(SHARE, b, s);
        // check there is an appropriate message body
        HelperClientMessageDeserializer messageDeserializer =
                HelperClientMessageDeserializer.newInstance(inputStream, STORESHARERESPONSEMESSAGE);
        Storeshare.StoreShareResponseMessage message = messageDeserializer.getBody().getStoreShareResponseMessage();
        ResultOuterClass.Result result = message.getResult();
        if (!result.getStatus().equals(OK)) {
            reporter.accept(false, result.getStatus() + " " + result.getMemo());
            return share;
        }
        if (message.getVersion() != share.version.versionNumber) {
            reporter.accept(false, "Version number is incorrect");
            return share;
        }
        reporter.accept(true, result.getStatus() + " " + result.getMemo());
        return share;
    }

    /**
     * Records the outcome status for the helper and for the notification message for pair and upnair success and
     * failure
     */
    private record PairingResponseProcessingStatus(DeRecHelperStatus.PairingStatus successStatus,
                                                   DeRecHelperStatus.PairingStatus failStatus,
                                                   DeRecStatusNotification.StandardNotificationType successNotification,
                                                   DeRecStatusNotification.StandardNotificationType failNotification) {
    }
}
