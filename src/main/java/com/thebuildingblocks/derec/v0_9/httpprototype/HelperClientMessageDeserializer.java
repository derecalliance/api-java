package com.thebuildingblocks.derec.v0_9.httpprototype;

import derec.message.Derecmessage;
import derec.message.Derecmessage.DeRecMessage;
import derec.message.Derecmessage.DeRecMessage.HelperMessageBody;
import derec.message.ResultOuterClass;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.stream.Collectors;

public class HelperClientMessageDeserializer {

    private final Map<HelperMessageBody.BodyCase, HelperMessageBody> bodyMessages;
    private final DeRecMessage message;
    private HelperMessageBody body;

    private HelperClientMessageDeserializer(InputStream inputStream) throws IOException {
        message = DeRecMessage.parseFrom(inputStream);
        bodyMessages = message.getMessageBodies()
                .getHelperMessageBodies()
                .getHelperMessageBodyList().stream().collect(Collectors.toMap(HelperMessageBody::getBodyCase, b -> b));

    }

    public <C> C getBodyMessage(HelperMessageBody.BodyCase bodyCase, Class<C> messageClass) {
        return getMessageBody(bodyMessages.get(bodyCase), messageClass);

    }

    public static <C> C getMessageBody(HelperMessageBody body, Class<C> klass) {
        return switch (body.getBodyCase()) {
            case PAIRRESPONSEMESSAGE -> klass.cast(body.getPairResponseMessage());
            case UNPAIRRESPONSEMESSAGE -> klass.cast(body.getUnpairResponseMessage());
            case STORESHARERESPONSEMESSAGE -> klass.cast(body.getStoreShareResponseMessage());
            case VERIFYSHARERESPONSEMESSAGE -> klass.cast(body.getVerifyShareResponseMessage());
            case GETSECRETIDSVERSIONSRESPONSEMESSAGE -> klass.cast(body.getGetSecretIdsVersionsResponseMessage());
            case GETSHARERESPONSEMESSAGE -> klass.cast(body.getGetShareResponseMessage());
            case BODY_NOT_SET -> throw new IllegalArgumentException("Body is not set for " + body.getBodyCase());
        };
    }

    public static ResultOuterClass.Result getResult(HelperMessageBody body) {
        return switch (body.getBodyCase()) {
            case PAIRRESPONSEMESSAGE -> body.getPairResponseMessage().getResult();
            case UNPAIRRESPONSEMESSAGE -> body.getUnpairResponseMessage().getResult();
            case STORESHARERESPONSEMESSAGE -> body.getStoreShareResponseMessage().getResult();
            case VERIFYSHARERESPONSEMESSAGE -> body.getVerifyShareResponseMessage().getResult();
            case GETSECRETIDSVERSIONSRESPONSEMESSAGE -> body.getGetSecretIdsVersionsResponseMessage().getResult();
            case GETSHARERESPONSEMESSAGE -> body.getGetShareResponseMessage().getResult();
            case BODY_NOT_SET -> throw new IllegalArgumentException("Body is not set for " + body.getBodyCase());
        };
    }

    /**
     * Check for one and only one response of the type indicated and make available via {@link #getBody()}
     * @param inputStream input stream containing the protobuf serialized messages
     * @param bodyCase type of message needed
     * @return a new instance containing the parsed message
     */
    public static HelperClientMessageDeserializer newInstance(InputStream inputStream,
                                                              HelperMessageBody.BodyCase bodyCase) {
        HelperClientMessageDeserializer instance = null;
        try {
            instance = new HelperClientMessageDeserializer(inputStream);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        if (instance.bodyMessages.isEmpty()) {
            throw new IllegalArgumentException("There are no bodies in the message");
        }
        if (instance.bodyMessages.size() > 1) {
            throw new IllegalArgumentException("There is more than one body in the message");
        }
        if (!instance.bodyMessages.containsKey(bodyCase)) {
            throw new IllegalArgumentException("The response is not a " + bodyCase);
        }
        instance.body = instance.bodyMessages.get(bodyCase);
        return instance;
    }

    public DeRecMessage getMessage() {
        return message;
    }

    public HelperMessageBody getBody() {
        return body;
    }

    public Map<HelperMessageBody.BodyCase, HelperMessageBody> getBodyMessages() {
        return bodyMessages;
    }
}
