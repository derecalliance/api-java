package com.thebuildingblocks.derec.v0_9.httpprototype;

import com.google.protobuf.ByteString;
import com.thebuildingblocks.derec.v0_9.interfaces.DeRecId;
import derec.message.*;
import derec.message.Derecmessage.DeRecMessage.SharerMessageBody;

import static derec.message.Communicationinfo.*;
import static derec.message.Derecmessage.*;
import static derec.message.Derecmessage.DeRecMessage.*;
import static derec.message.Pair.*;
import static derec.message.Storeshare.*;
import static derec.message.Unpair.*;
import static derec.message.Verify.*;

public class HelperClientMessageFactory {

    public static SharerMessageBody getPairRequestMessageBody (DeRecId deRecId) {
        return SharerMessageBody.newBuilder()
                .setPairRequestMessage(PairRequestMessage.newBuilder()
                        .setCommunicationInfo(CommunicationInfo.newBuilder()
                                .addCommunicationInfoEntries(CommunicationInfoKeyValue.newBuilder()
                                        .setKey("email")
                                        .setStringValue(deRecId.getContact().toString()))
                                .addCommunicationInfoEntries(CommunicationInfoKeyValue.newBuilder()
                                        .setKey("address")
                                        .setStringValue(deRecId.getAddress().toString()))
                                .addCommunicationInfoEntries(CommunicationInfoKeyValue.newBuilder()
                                        .setKey("name")
                                        .setStringValue(deRecId.getName()))
                                .build())
                        .build())
                .build();
    }

    public static SharerMessageBody getShareRequestMessageBody (Version.Share share) {
        ByteString bytes = DeRecShare.newBuilder()
                .setVersion(share.version.versionNumber)
                .build()
                .toByteString();
        return SharerMessageBody.newBuilder()
                .setStoreShareRequestMessage(StoreShareRequestMessage.newBuilder()
                        .setCommittedDeRecShare(CommittedDeRecShare.newBuilder()
                                .setDeRecShare(bytes)
                                .build())
                        .build())
                .build();
    }

    public static SharerMessageBody getVerifyRequestMessageBody (Version.Share share) {
        return SharerMessageBody.newBuilder()
                .setVerifyShareRequestMessage(VerifyShareRequestMessage.newBuilder()
                        .setNonce(ByteString.copyFrom(share.nonce))
                        .setVersion(share.version.versionNumber)
                        .build())
                .build();
    }

    public static SharerMessageBody getUnPairRequestMessageBody (String reason) {
        return SharerMessageBody.newBuilder()
                .setUnpairRequestMessage(UnpairRequestMessage.newBuilder()
                        .setMemo(reason)
                        .build())
                .build();
    }


    public static DeRecMessage getMessage(SharerMessageBody body) {
        return newBuilder()
                .setMessageBodies(MessageBodies.newBuilder()
                        .setSharerMessageBodies(SharerMessageBodies.newBuilder()
                                .addSharerMessageBody(body)
                                .build())
                        .build())
                .build();
    }
}
