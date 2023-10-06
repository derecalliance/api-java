package com.thebuildingblocks.derec.v0_9.httpprototype;

import com.thebuildingblocks.derec.v0_9.interfaces.DeRecPairable;
import com.thebuildingblocks.derec.v0_9.interfaces.DeRecStatusNotification;
import derec.message.Derecmessage;

import java.util.HashMap;
import java.util.Map;

import static com.thebuildingblocks.derec.v0_9.interfaces.DeRecPairable.PairingStatus.*;
import static com.thebuildingblocks.derec.v0_9.interfaces.DeRecStatusNotification.Type.*;

public class ResponseProcessing {
    private final static Map<Derecmessage.DeRecMessage.HelperMessageBody.BodyCase, ResponseProcessingStatus> responsesStatuses = new HashMap<>();
    static ResponseProcessingStatus unPairStatus = new ResponseProcessingStatus(REMOVED, FAILED, HELPER_UNPAIRED, HELPER_INACTIVE);
    static ResponseProcessingStatus pairStatus = new ResponseProcessingStatus(PAIRED, REFUSED, HELPER_READY, HELPER_NOT_PAIRED);

    static {
        responsesStatuses.put(Derecmessage.DeRecMessage.HelperMessageBody.BodyCase.PAIRRESPONSEMESSAGE, pairStatus);
        responsesStatuses.put(Derecmessage.DeRecMessage.HelperMessageBody.BodyCase.UNPAIRRESPONSEMESSAGE, unPairStatus);
    }

    public static ResponseProcessingStatus getResponseProcessingStatus(Derecmessage.DeRecMessage.HelperMessageBody.BodyCase bodyCase) {
        return responsesStatuses.get(bodyCase);
    }

    public record ResponseProcessingStatus(DeRecPairable.PairingStatus successStatus,
                                           DeRecPairable.PairingStatus failStatus,
                                           DeRecStatusNotification.Type successNotification,
                                           DeRecStatusNotification.Type failNotification){}
}
