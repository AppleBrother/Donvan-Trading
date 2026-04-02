package com.example.donvan.telegram;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelegramBotSenderTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void sendMessage_postsExpectedPayload() throws Exception {
        AtomicReference<String> requestMethod = new AtomicReference<>();
        AtomicReference<String> requestPath = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();

        try (LocalTelegramServer server = new LocalTelegramServer(exchange -> {
            requestMethod.set(exchange.getRequestMethod());
            requestPath.set(exchange.getRequestURI().getPath());
            requestBody.set(readBody(exchange));
            writeJson(exchange, 200, "{\"ok\":true,\"result\":{\"message_id\":1}}");
        })) {
            TelegramBotSender sender = new TelegramBotSender(
                    objectMapper,
                    HttpClient.newHttpClient(),
                    server.baseUrl()
            );

            sender.sendMessage("123456:fakeToken", "987654321", "hello telegram");
        }

        assertEquals("POST", requestMethod.get());
        assertEquals("/bot123456:fakeToken/sendMessage", requestPath.get());

        Map<String, Object> payload = objectMapper.readValue(requestBody.get(), new TypeReference<>() {
        });
        assertEquals("987654321", payload.get("chat_id"));
        assertEquals("hello telegram", payload.get("text"));
    }

    @Test
    void sendMessage_throwsWhenTelegramRejectsRequest() throws Exception {
        try (LocalTelegramServer server = new LocalTelegramServer(exchange ->
                writeJson(exchange, 200, "{\"ok\":false,\"error_code\":400,\"description\":\"chat not found\"}"))) {
            TelegramBotSender sender = new TelegramBotSender(
                    objectMapper,
                    HttpClient.newHttpClient(),
                    server.baseUrl()
            );

            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> sender.sendMessage("123456:fakeToken", "bad-chat", "hello telegram"));

            assertTrue(exception.getMessage().contains("chat not found"));
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void writeJson(HttpExchange exchange, int statusCode, String responseBody) throws IOException {
        byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
        exchange.close();
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }

    private static final class LocalTelegramServer implements AutoCloseable {

        private final HttpServer server;

        private LocalTelegramServer(ExchangeHandler handler) throws IOException {
            this.server = HttpServer.create(new InetSocketAddress(0), 0);
            this.server.createContext("/", exchange -> {
                try {
                    handler.handle(exchange);
                } finally {
                    exchange.close();
                }
            });
            this.server.start();
        }

        private String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
