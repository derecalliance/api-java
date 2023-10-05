package com.thebuildingblocks.derec.v0_9.test;

import com.thebuildingblocks.derec.v0_9.httpprototype.Sharer;
import com.thebuildingblocks.derec.v0_9.interfaces.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;

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
        Sharer me = Sharer.newBuilder()
                .id(new DeRecId("Secret Sammy", "mailto:test@example.org", null))
                .notificationListener(this::logNotification)
                .build();
        // get a secret
        logger.info("Building a secret, wait for it to be available");
        DeRecSecret secret = me.newSecret("Martin Luther", "I have a dream".getBytes(StandardCharsets.UTF_8),
                Arrays.asList(DEFAULT_IDS));
        // get last version shared - in this case the first version shared
        DeRecVersion v = secret.getVersions().lastEntry().getValue();
        logger.info("Secret version: {}, is protected: {}", v.getVersionNumber(), v.isProtected());

        // update the secret
        logger.info("Updating the secret");
        v = secret.update("I have another dream".getBytes(StandardCharsets.UTF_8));
        logger.info("Secret version: {}, is protected {}", v.getVersionNumber(), v.isProtected());

        logger.info("Closing secret {}", secret.getSecretId());
        // dispose of it
        secret.close();


        try {
            // should not be able to update after close
            secret.update("throw me an exception".getBytes(StandardCharsets.UTF_8));
            throw new AssertionError("can't update after close");
        } catch (IllegalStateException e) {
            // correctly throwing exception
            logger.info("[Expected] Exception on update secret", e);
        }

        DeRecSecret secret2 = me.newSecret("Genghis Khan", "Something".getBytes(StandardCharsets.UTF_8),
                Arrays.asList(DEFAULT_IDS));

        System.out.println("Helpers and Secrets");
        Recipes.listHelpers(me).forEach((key, value) -> {
            System.out.println(key.getName());
            value.forEach(s -> System.out.printf("   Secret id: %s, \"%s\", Closed: %b, Available: %b\n",
                    s.getSecretId(), s.getDescription(), s.isClosed(), s.isAvailable()));
        });

        System.out.println("Secrets and Helpers");
        for (DeRecSecret s: me.getSecrets()) {
            System.out.println("Secret Id: " + s.getSecretId().toString());
            for (DeRecPairable p: s.getHelpers()) {
                System.out.println("   " + p.getId().getName() + ": " + p.getStatus());
            }
        }
        System.out.println("Hit enter to exit");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
        me.close();
    }

    private void logNotification(DeRecStatusNotification t) {
        String v =t.getVersion().isEmpty() ? "" : "/" + t.getVersion().get().getVersionNumber();
        String p = t.getPairable().isEmpty() ? "" : "/" + t.getPairable().get().getId().getName();
        logger.info("\u001B[34m{} {} {}{} {}\u001B[0m", t.getType(), p,
                t.getSecret().getSecretId(), v, t.getMessage());
    }
}
