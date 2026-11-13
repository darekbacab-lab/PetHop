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
        String body = "{\"sender\":{\"name\":\"PetHop\",\"email\":" + json(properties.getFrom()) + "},\"to\":[{\"email\":" + json(to) + "}],"
                + "\"subject\":" + json(subject) + ",\"textContent\":" + json(text)
                + ",\"htmlContent\":" + json(htmlContent(subject, text)) + "}";
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

    /**
     * Versión HTML del correo con la marca de PetHop: encabezado en color, el enlace como botón
     * y el enlace en texto por si el botón no funciona. El texto plano se sigue enviando como
     * respaldo para los clientes que no muestran HTML.
     */
    private String htmlContent(String subject, String text) {
        String actionLabel = subject.replace(" de PetHop", "");
        StringBuilder paragraphs = new StringBuilder();
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                paragraphs.append("<p style=\"text-align:center;margin:24px 0;\"><a href=\"")
                        .append(escapeHtml(trimmed))
                        .append("\" style=\"display:inline-block;background:#167F87;color:#ffffff;text-decoration:none;font-weight:bold;padding:12px 28px;border-radius:8px;\">")
                        .append(escapeHtml(actionLabel)).append("</a></p>")
                        .append("<p style=\"font-size:13px;color:#6b7280;text-align:center;\">Si el botón no funciona, copia este enlace en tu navegador:<br><a href=\"")
                        .append(escapeHtml(trimmed)).append("\" style=\"color:#167F87;word-break:break-all;\">")
                        .append(escapeHtml(trimmed)).append("</a></p>");
            } else {
                paragraphs.append("<p style=\"margin:0 0 16px;\">").append(escapeHtml(trimmed)).append("</p>");
            }
        }
        return "<!doctype html><html><body style=\"margin:0;padding:0;background:#f3f4f6;\">"
                + "<div style=\"max-width:560px;margin:0 auto;padding:24px 16px;font-family:Arial,Helvetica,sans-serif;color:#1f2937;\">"
                + "<div style=\"background:#167F87;color:#ffffff;text-align:center;padding:20px;border-radius:12px 12px 0 0;font-size:24px;font-weight:bold;\">&#128062; PetHop</div>"
                + "<div style=\"background:#ffffff;padding:28px 24px;border-radius:0 0 12px 12px;\">"
                + "<h1 style=\"font-size:20px;margin:0 0 16px;color:#111827;\">" + escapeHtml(subject) + "</h1>"
                + paragraphs
                + "</div>"
                + "<p style=\"font-size:12px;color:#6b7280;text-align:center;margin:16px 0 0;\">PetHop — adopciones responsables</p>"
                + "</div></body></html>";
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String json(String value) {
        String escaped = value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
        return "\"" + escaped + "\"";
    }
}
