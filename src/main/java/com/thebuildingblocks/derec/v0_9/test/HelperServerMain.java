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

package com.thebuildingblocks.derec.v0_9.test;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.thebuildingblocks.derec.v0_9.interfaces.DeRecId;
import derec.message.*;
import derec.message.Communicationinfo.CommunicationInfoKeyValue;
import derec.message.Derecmessage.DeRecMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.Collectors;

import static com.thebuildingblocks.derec.v0_9.httpprototype.HelperServerMessageFactory.*;
import static com.thebuildingblocks.derec.v0_9.test.TestIds.DEFAULT_IDS;
import static derec.message.Derecmessage.DeRecMessage.*;
import static derec.message.Storeshare.*;
import static derec.message.Unpair.*;
import static derec.message.Verify.*;

public class HelperServerMain {
    static Logger logger = LoggerFactory.getLogger(HelperServerMain.class);
    static HttpServer server;

    public static void main(String[] args) throws IOException {
        server = HttpServer.create(new InetSocketAddress(8080), 10);
        for (DeRecId id : DEFAULT_IDS) {
            if (id.getName().startsWith("no")) {
                continue;
            }
            HttpContext context = server.createContext(id.getAddress().getPath(), new HelperHandler(id));
            logger.info("Started helper {}", context.getPath());
        }
        logger.info("Server started");
        server.start();
        System.out.println("Hit enter to exit");
        Scanner sc = new Scanner(System.in);
        sc.nextLine();
        server.stop(0);
        System.out.println("Server stopped");
    }

    public static class HelperHandler implements HttpHandler {
        private final DeRecId id;

        public HelperHandler(DeRecId id) {
            this.id = id;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                DeRecMessage message = parseFrom(exchange.getRequestBody());
                SharerMessageBodies messageBodies =
                        message.getMessageBodies().getSharerMessageBodies();
                List<HelperMessageBody> responses = new ArrayList<>();
                for (SharerMessageBody messageBody : messageBodies.getSharerMessageBodyList()) {
                    HelperMessageBody response = switch (messageBody.getBodyCase()) {
                        case PAIRREQUESTMESSAGE -> processPairRequest(message, messageBody.getPairRequestMessage());
                        case UNPAIRREQUESTMESSAGE -> processUnPairRequest(message, messageBody.getUnpairRequestMessage());
                        case STORESHAREREQUESTMESSAGE -> processShareRequest(message, messageBody.getStoreShareRequestMessage());
                        case VERIFYSHAREREQUESTMESSAGE -> processVerifyShareRequestMessage(message, messageBody.getVerifyShareRequestMessage());
                        default -> {
                            logger.warn("Unhandled message received {}", messageBody.getBodyCase());
                            exchange.sendResponseHeaders(400, -1);
                            yield null;
                        }
                    };
                    if (Objects.nonNull(response)) {
                        responses.add(response);
                    }
                }

                DeRecMessage reply = DeRecMessage.newBuilder()
                        .setMessageBodies(MessageBodies.newBuilder()
                                .setHelperMessageBodies(HelperMessageBodies.newBuilder()
                                        .addAllHelperMessageBody(responses)
                                        .build())
                                .build())
                        .build();
                exchange.sendResponseHeaders(200, 0);
                try (OutputStream os = exchange.getResponseBody()) {
                    reply.writeTo(exchange.getResponseBody());
                }
            } catch (Throwable t) {
                logger.error("Exception processing message: {}", t.getMessage());
            }
        }

        private HelperMessageBody processVerifyShareRequestMessage(DeRecMessage message,
                                                                   VerifyShareRequestMessage verifyShareRequestMessage) {
            logger.info("{} received {}", id.getName(), verifyShareRequestMessage.getClass().getSimpleName());
            return getVerifyShareResponseMessageBody(verifyShareRequestMessage.getNonce().toByteArray());
        }

        private HelperMessageBody processShareRequest(DeRecMessage message,
                                                      StoreShareRequestMessage storeShareRequestMessage) throws InvalidProtocolBufferException {
            logger.info("{} received {}", id.getName(), storeShareRequestMessage.getClass().getSimpleName());
            ByteString shareBytes = storeShareRequestMessage.getCommittedDeRecShare().getDeRecShare();
            DeRecShare share = DeRecShare.parseFrom(shareBytes);
            logger.info("Share version was {}", share.getVersion());
            return getShareResponseMessageBody(ResultOuterClass.StatusEnum.OK, "Share stored", share.getVersion());
        }

        private HelperMessageBody processUnPairRequest(DeRecMessage message,
                                                       UnpairRequestMessage unpairRequestMessage) {
            logger.info("{} received {}", id.getName(), unpairRequestMessage.getClass().getSimpleName());
            return getUnpairResponseMessageBody();
        }

        private HelperMessageBody processPairRequest(DeRecMessage message, Pair.PairRequestMessage pairRequestMessage) {
            Map<String, Object> communicationInfo =
                    pairRequestMessage.getCommunicationInfo().getCommunicationInfoEntriesList()
                    .stream()
                    .collect(Collectors.toMap(CommunicationInfoKeyValue::getKey,
                            kv -> kv.hasStringValue() ? kv.getStringValue() : kv.getBytesValue()));
            logger.info("{} received {} \"{}\"", id.getName(), pairRequestMessage.getClass().getSimpleName(),
                    communicationInfo.get("name"));
            return getPairResponseMessageBody();
        }
    }
}
