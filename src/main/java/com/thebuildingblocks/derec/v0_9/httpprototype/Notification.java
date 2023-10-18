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
import com.thebuildingblocks.derec.v0_9.interfaces.DeRecSecret;
import com.thebuildingblocks.derec.v0_9.interfaces.DeRecStatusNotification;
import com.thebuildingblocks.derec.v0_9.interfaces.DeRecVersion;

import java.util.Optional;

public class Notification implements DeRecStatusNotification {
    Type type;
    String message = "";
    DeRecVersion version;
    DeRecHelperStatus pairable;
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
    public Optional<DeRecHelperStatus> getPairable() {
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

        public Builder pairable(DeRecHelperStatus pairable) {
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
