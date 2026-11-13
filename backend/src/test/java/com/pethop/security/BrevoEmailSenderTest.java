package com.pethop.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BrevoEmailSenderTest {
    private HttpServer server;
    private AtomicReference<String> apiKeyHeader;
    private AtomicReference<String> body;
    private int status = 201;

    @BeforeEach
    void startServer() throws IOException {
        apiKeyHeader = new AtomicReference<>();
        body = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v3/smtp/email", exchange -> {
            apiKeyHeader.set(exchange.getRequestHeaders().getFirst("api-key"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, response.length);
            try (OutputStream out = exchange.getResponseBody()) { out.write(response); }
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void sendsEmailThroughBrevoApi() {
        BrevoEmailSender sender = sender();
        sender.send("adoptante@example.test", "Confirma tu correo de PetHop",
                "Hola, gracias por registrarte en PetHop.\n\nhttps://pethop-production.up.railway.app/acceso.html?mode=verify&token=abc");
        assertThat(apiKeyHeader.get()).isEqualTo("xkeysib-test");
        assertThat(body.get()).contains("\"sender\":{\"name\":\"PetHop\",\"email\":\"pethop13@gmail.com\"}");
        assertThat(body.get()).contains("\"to\":[{\"email\":\"adoptante@example.test\"}]");
        assertThat(body.get()).contains("\"subject\":\"Confirma tu correo de PetHop\"");
        assertThat(body.get()).contains("\"textContent\":\"Hola, gracias por registrarte en PetHop.");
        assertThat(body.get()).contains("\"htmlContent\":\"");
        assertThat(body.get()).contains(">Confirma tu correo</a>");
        assertThat(body.get()).contains("href=\\\"https://pethop-production.up.railway.app/acceso.html?mode=verify&amp;token=abc\\\"");
        assertThat(body.get()).contains("background:#167F87");
    }

    @Test
    void rejectsNonSuccessfulResponses() {
        status = 401;
        assertThatThrownBy(() -> sender().send("adoptante@example.test", "Asunto", "Texto"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void isNotConfiguredWithoutApiKey() {
        BrevoProperties properties = new BrevoProperties();
        properties.setApiKey("   ");
        properties.setFrom("pethop13@gmail.com");
        BrevoEmailSender sender = new BrevoEmailSender(properties);
        assertThat(sender.isConfigured()).isFalse();
        assertThatThrownBy(() -> sender.send("adoptante@example.test", "Asunto", "Texto"))
                .isInstanceOf(IllegalStateException.class);
    }

    private BrevoEmailSender sender() {
        BrevoProperties properties = new BrevoProperties();
        properties.setApiKey("xkeysib-test");
        properties.setFrom("pethop13@gmail.com");
        properties.setApiUrl("http://localhost:" + server.getAddress().getPort() + "/v3/smtp/email");
        return new BrevoEmailSender(properties);
    }
}
