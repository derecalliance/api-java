package com.thebuildingblocks.derec.v0_9.test;

import com.thebuildingblocks.derec.v0_9.httpprototype.Sharer;
import com.thebuildingblocks.derec.v0_9.interfaces.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static com.thebuildingblocks.derec.v0_9.test.TestIds.DEFAULT_IDS;

/**
 * Illustration of use of classes
 */
public class SharerMain {
    static Logger logger = LoggerFactory.getLogger(SharerMain.class);

    public static void main(String[] args) {
        new SharerMain().run();
    }

    public void run() {
        // build a sharer
        DeRecSharer me = new Sharer.Builder()
                .id(new DeRecId("Secret Sammy", "mailto:test@example.org", null))
                .notificationListener(this::logNotification)
                .build();
        // get a secret
        DeRecSecret secret = me.newSecret("Martin Luther", "I have a dream".getBytes(StandardCharsets.UTF_8),
                Arrays.asList(DEFAULT_IDS));
        // get last version shared - in this case the first version shared
        DeRecVersion v = secret.getVersions().lastEntry().getValue();
        logger.info("Secret version {}, {}", v.getVersionNumber(), v.isProtected());

        // update the secret
        v = secret.update("I have another dream".getBytes(StandardCharsets.UTF_8));
        logger.info("Secret version {}, {}", v.getVersionNumber(), v.isProtected());

        logger.info("Closing secret {}", secret.getSecretId());
        // dispose of it
        secret.close();


        try {
            // should not be able to update after close
            secret.update("throw me an exception".getBytes(StandardCharsets.UTF_8));
            throw new AssertionError("can't update after close");
        } catch (IllegalStateException e) {
            // correctly throwing exception
            logger.info("Exception on update secret", e);
        }

        DeRecSecret secret2 = me.newSecret("Genghis Khan", "Something".getBytes(StandardCharsets.UTF_8),
                Arrays.asList(DEFAULT_IDS));

        System.out.println("Helpers and Secrets");
        Recipes.listHelpers(me).forEach((key, value) -> {
            System.out.println(key.getName());
            value.forEach(s -> System.out.println(s.getSecretId() + ": \"" + s.getDescription() + "\" Available: " + s.isAvailable()));
        });

    }

    private void logNotification(DeRecStatusNotification t) {
        logger.info("\u001B[34m Status Notification: {} {}\u001B[0m", t.getType(), t.getMessage());
    }
}
