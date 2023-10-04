package com.thebuildingblocks.derec.v0_9.test;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.thebuildingblocks.derec.v0_9.interfaces.DeRecId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static com.thebuildingblocks.derec.v0_9.test.TestIds.DEFAULT_IDS;

public class HelperServerMain {
    static Logger logger = LoggerFactory.getLogger("helper");
    static HttpServer server;

    public static void main(String[] args) throws IOException {
        server = HttpServer.create(new InetSocketAddress(8080), 10);
        for (DeRecId id : DEFAULT_IDS) {
            if (id.getName().startsWith("no")) {
                continue;
            }
            HttpContext context = server.createContext(id.getAddress().getPath(), new HelperHandler(id));
            logger.info("Started helper {}", context.getPath());
        }
        logger.info("Server started");
        server.start();
        System.out.println("Hit enter to exit");
        Scanner sc = new Scanner(System.in);
        sc.nextLine();
        server.stop(0);
        System.out.println("Server stopped");
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
            logger.info("{} received \"{}\"", id.getName(), new String(input, StandardCharsets.UTF_8));
            try (OutputStreamWriter ow = new OutputStreamWriter(exchange.getResponseBody())) {
                ow.append("I am ")
                        .append(id.getName())
                        .append("\n")
                        .append("Hello World!");
            }
        }
    }

}
