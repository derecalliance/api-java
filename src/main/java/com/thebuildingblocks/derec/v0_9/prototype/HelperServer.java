package com.thebuildingblocks.derec.v0_9.prototype;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Scanner;

/**
 * Stub implementation of a HelperServer, just responds OK
 */
public class HelperServer {

    /**
     * Placeholder for information a helper knows about itself
     */
    public static class HelperModel {
        DeRecId id; // helper's Id
        KeyPair keyPair; // public/private key pair
        X509Certificate certificate; // certificate to use
        URI address; // my transport address
        List<String> availableVersions; // a list of available protocol versions
        Util.RetryParameters retryParameters; // parameters to negotiate for any sharer/secret
        List<Sharer> sharer; // list of paired sharers

        /**
         * Information a helper know about a sharer
         */
        public static class Sharer {
            DeRecId sharerId; // sharer unique id
            URI sharerAddress; // sharer transport address
            PublicKey publicKey; // sharer's public key
            URI tsAndCs;    // link to legal conditions regarding what the helper is to do about
            // authentication for recovery and substitution of sharer
            List<Integer> keepList; // the shares to keep (these can come from any secret)
            List<Share> shares; // the kept shares
        }

        /**
         * Information a helper knows about a share
         */
        public static class Share {
            byte[] shareContent; // contents of the share
            int shareVersion; // the version of the share
            byte[] signature; // the signature attached to the share
        }
    }

    static Logger logger = LoggerFactory.getLogger("helper");
    static HttpServer server;

    public static void main(String[] args) throws IOException {
        System.out.println("Hit enter to exit");
        server = HttpServer.create(new InetSocketAddress(8080), 10);
        for (DeRecId id : DeRecId.DEFAULT_IDS) {
            HttpContext context = server.createContext(id.address.getPath(), new HelperHandler(id));
            logger.info("Started {}", context.getPath());
        }
        server.start();
        Scanner sc = new Scanner(System.in);
        sc.nextLine();
        server.stop(0);
    }

    public static class HelperHandler implements HttpHandler {
        private final DeRecId id;

        public HelperHandler(DeRecId id) {
            this.id = id;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.sendResponseHeaders(200, 0);
            byte [] input = exchange.getRequestBody().readAllBytes();
            logger.info("{} received \"{}\"", id.name, new String(input, StandardCharsets.UTF_8));
            try (OutputStreamWriter ow = new OutputStreamWriter(exchange.getResponseBody())) {
                ow.append("I am ")
                        .append(id.name)
                        .append("\n")
                        .append("Hello World!");
            }
        }
    }
}
