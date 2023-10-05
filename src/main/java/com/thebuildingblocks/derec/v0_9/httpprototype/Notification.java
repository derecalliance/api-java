package com.thebuildingblocks.derec.v0_9.httpprototype;

import com.thebuildingblocks.derec.v0_9.interfaces.DeRecPairable;
import com.thebuildingblocks.derec.v0_9.interfaces.DeRecSecret;
import com.thebuildingblocks.derec.v0_9.interfaces.DeRecStatusNotification;
import com.thebuildingblocks.derec.v0_9.interfaces.DeRecVersion;

import java.util.Optional;

public class Notification implements DeRecStatusNotification {
    Type type;
    String message = "";
    DeRecVersion version;
    DeRecPairable pairable;
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
    public Optional<DeRecVersion> getVersion() {
        return Optional.ofNullable(version);
    }

    @Override
    public Optional<DeRecPairable> getPairable() {
        return Optional.ofNullable(pairable);
    }

    @Override
    public DeRecSecret getSecret() {
        return secret;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private final Notification notification;

        protected Builder(){
            notification = new Notification();
        }

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

        public Builder pairable(DeRecPairable pairable) {
            notification.pairable = pairable;
            return this;
        }

        public Builder secret(DeRecSecret secret) {
            notification.secret = secret;
            return this;
        }

        public Notification build(){
            return notification;
        }

        public Notification build(Type type){
            return this.type(type).build();
        }
    }
}
