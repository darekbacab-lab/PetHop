package com.pethop.security;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Envía correos transaccionales con la API HTTP de Brevo. Railway bloquea SMTP saliente en los
 * planes gratuitos, así que el envío va por HTTPS (puerto 443) con la clave de API de Brevo.
 */
@Service
public class BrevoEmailSender implements TransactionalEmailSender {
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private final BrevoProperties properties;
    private final HttpClient http;

    @Autowired
    public BrevoEmailSender(BrevoProperties properties) {
        this(properties, HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
    }

    BrevoEmailSender(BrevoProperties properties, HttpClient http) {
        this.properties = properties;
        this.http = http;
    }

    @Override
    public boolean isConfigured() {
        return properties.getApiKey() != null && !properties.getApiKey().isBlank()
                && properties.getFrom() != null && !properties.getFrom().isBlank();
    }

    @Override
    public void send(String to, String subject, String text) {
        if (!isConfigured()) throw new IllegalStateException("El envío de correo por Brevo no está configurado.");
        String body = "{\"sender\":{\"email\":" + json(properties.getFrom()) + "},\"to\":[{\"email\":" + json(to) + "}],"
                + "\"subject\":" + json(subject) + ",\"textContent\":" + json(text) + "}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getApiUrl()))
                .timeout(TIMEOUT)
                .header("api-key", properties.getApiKey())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Brevo rechazó el correo (HTTP " + response.statusCode() + ").");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("No fue posible contactar a Brevo.", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("No fue posible contactar a Brevo.", ex);
        }
    }

    private String json(String value) {
        String escaped = value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
        return "\"" + escaped + "\"";
    }
}
