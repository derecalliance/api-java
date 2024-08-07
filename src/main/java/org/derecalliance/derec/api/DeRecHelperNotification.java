package org.derecalliance.derec.api;

/**
 * Notifications related to the helper
 */
public interface DeRecHelperNotification {
    Type getType(); // the type of the notification

    DeRecIdentity getSharerId(); // the sharer id

    DeRecSecret.Id getSecretId(); // the secretId or null if none

    int getVersion(); // the version number or -1 if inapplicable

    /**
     * The type of the notification - allows for introduction of custom helper notifications
     * aside from {@link StandardHelperNotificationType}
     */
    interface Type {
        String name();
    }

    enum StandardHelperNotificationType implements Type {
        PAIR_INDICATION, // someone is trying to pair for a particular secret
        UNPAIR_INDICATION, // someone is unpairing for a particular secret
        UPDATE_INDICATION, // an update has been received for a secret
        VERIFY_INDICATION, // a verification request has been received for a secret
        LIST_SECRETS_INDICATION, // a request to list secrets has been received
        RECOVER_SECRET_INDICATION // a request to recover a secret has been received
    }
}
