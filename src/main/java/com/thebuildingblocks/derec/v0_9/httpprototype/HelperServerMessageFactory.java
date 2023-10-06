package com.thebuildingblocks.derec.v0_9.httpprototype;

import com.google.protobuf.ByteString;
import derec.message.*;
import derec.message.Derecmessage.DeRecMessage.HelperMessageBody;

import static derec.message.Pair.*;
import static derec.message.ResultOuterClass.*;
import static derec.message.Storeshare.*;
import static derec.message.Unpair.*;
import static derec.message.Verify.*;

public class HelperServerMessageFactory {

    public static HelperMessageBody getShareResponseMessageBody(StatusEnum status, String message, long version){
        return HelperMessageBody.newBuilder()
                .setStoreShareResponseMessage(StoreShareResponseMessage.newBuilder()
                        .setVersion(version)
                        .setResult(Result.newBuilder()
                                .setMemo(message)
                                .setStatus(status)
                                .build())
                        .build())
                .build();
    }

    public static HelperMessageBody getVerifyShareResponseMessageBody(byte[] nonce){
        return HelperMessageBody.newBuilder()
                .setVerifyShareResponseMessage(VerifyShareResponseMessage.newBuilder()
                        .setNonce(ByteString.copyFrom(nonce))
                        .setResult(Result.newBuilder()
                                .setStatus(StatusEnum.OK)
                                .build())
                        .build())
                .build();
    }

    public static HelperMessageBody getUnpairResponseMessageBody () {
        return HelperMessageBody.newBuilder()
                .setUnpairResponseMessage(UnpairResponseMessage.newBuilder()
                        .setResult(Result.newBuilder()
                                .setStatus(StatusEnum.OK)
                                .build())
                        .build())
                .build();
    }

    public static HelperMessageBody getPairResponseMessageBody () {
        return HelperMessageBody.newBuilder()
                .setPairResponseMessage(PairResponseMessage.newBuilder()
                        .setResult(Result.newBuilder()
                                .setStatus(StatusEnum.OK)
                                .build())
                        .build())
                .build();
    }
}
