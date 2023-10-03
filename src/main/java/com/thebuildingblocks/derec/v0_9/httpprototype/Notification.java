package com.thebuildingblocks.derec.v0_9.httpprototype;

import com.thebuildingblocks.derec.v0_9.interfaces.DeRecSecret;
import com.thebuildingblocks.derec.v0_9.interfaces.DeRecStatusNotification;
import com.thebuildingblocks.derec.v0_9.interfaces.DeRecVersion;

public class Notification implements DeRecStatusNotification {
    Type type;
    String message;
    DeRecVersion version;
    DeRecSecret secret;

    private Notification() {}

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public DeRecVersion getVersion() {
        return version;
    }

    @Override
    public DeRecSecret getSecret() {
        return secret;
    }

    public static class Builder {
        Notification notification = new Notification();

        public Builder message(String message) {
            notification.message = message;
            return this;
        }

        public Builder type(Type type) {
            notification.type = type;
            return this;
        }

        public Builder version(DeRecVersion version) {
            notification.version = version;
            return this;
        }

        public Builder secret(DeRecSecret secret) {
            notification.secret = secret;
            return this;
        }

        public Notification build(){
            return notification;
        }
    }
}
