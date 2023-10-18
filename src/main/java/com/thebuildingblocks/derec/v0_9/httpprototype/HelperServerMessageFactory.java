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
