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

import com.thebuildingblocks.derec.v0_9.httpprototype.Secret;
import com.thebuildingblocks.derec.v0_9.httpprototype.Util;
import com.thebuildingblocks.derec.v0_9.interfaces.DeRecStatusNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Notifier {
    private static final Logger logger = LoggerFactory.getLogger(Notifier.class);
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";
    static void logNotification(DeRecStatusNotification t) {
        String v = t.getVersion().isEmpty() ? "" : "/" + t.getVersion().get().getVersionNumber();
        String p = t.getHelper().isEmpty() ? "" : "/" + t.getHelper().get().getId().getName();
        Secret s = ((Secret) t.getSecret());
        String color = switch (t.getSeverity()) {
            case UNCLASSIFIED -> ANSI_BLUE;
            case NORMAL -> ANSI_GREEN;
            case WARNING -> ANSI_YELLOW;
            case ERROR -> ANSI_RED;
        };
        logger.info("{}{} {} {}{} {}\u001B[0m", color, t.getType(), p,
                s.getSecretIdAsUuid(), v, t.getMessage());
    }

}
