package com.pethop.veterinary;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pethop.common.exception.ApiException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OpenAiCompatibleVeterinaryAiProvider implements VeterinaryAiProvider {
    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleVeterinaryAiProvider.class);
    private final ObjectMapper mapper;
    private final VeterinaryAiProperties properties;
    private final HttpClient httpClient;

    @Autowired
    public OpenAiCompatibleVeterinaryAiProvider(ObjectMapper mapper, VeterinaryAiProperties properties) {
        this(mapper, properties, createHttpClient(properties));
    }

    OpenAiCompatibleVeterinaryAiProvider(ObjectMapper mapper, VeterinaryAiProperties properties, HttpClient httpClient) {
        this.mapper = mapper; this.properties = properties; this.httpClient = httpClient;
    }

    @Override
    public String generate(String prompt) {
        boolean keyPresent = properties.getApiKey() != null && !properties.getApiKey().isBlank();
        if (!properties.isEnabled() || !keyPresent) {
            log.warn("Veterinary AI unavailable before HTTP call enabled={} apiKeyPresent={} model={} endpoint={}",
                    properties.isEnabled(), keyPresent, properties.getModel(), safeEndpoint());
            throw ApiException.serviceUnavailable("El asistente veterinario no está disponible en este momento.");
        }
        try {
            log.info("Veterinary AI HTTP call started model={} endpoint={} promptLength={}", properties.getModel(), safeEndpoint(), prompt == null ? 0 : prompt.length());
            Map<String, Object> requestBody = new java.util.LinkedHashMap<>();
            requestBody.put("model", properties.getModel());
            requestBody.put("temperature", 0.1);
            requestBody.put("messages", new Object[] {
                    Map.of("role", "system", "content", systemInstruction()),
                    Map.of("role", "user", "content", prompt)
            });
            if (isGroqEndpoint()) {
                requestBody.put("max_completion_tokens", properties.getMaxOutputTokens());
                requestBody.put("reasoning_effort", "low");
                requestBody.put("response_format", Map.of("type", "json_object"));
            } else {
                requestBody.put("max_tokens", properties.getMaxOutputTokens());
            }
            String body = mapper.writeValueAsString(requestBody);
            HttpRequest request = HttpRequest.newBuilder(URI.create(properties.getBaseUrl()))
                    .timeout(Duration.ofSeconds(Math.max(1, properties.getReadTimeoutSeconds())))
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = sendWithRetry(request);
            log.info("Veterinary AI HTTP response status={} responseBytes={}", response.statusCode(), response.body() == null ? 0 : response.body().length());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                ProviderError providerError = parseProviderError(response.statusCode(), response.body());
                log.warn("Veterinary AI HTTP request rejected status={} category={} providerCode={} providerStatus={} providerMessage={}",
                        response.statusCode(), providerError.category(), providerError.code(), providerError.status(), providerError.message());
                throw ApiException.serviceUnavailable("El servicio veterinario no está disponible en este momento.");
            }
            JsonNode content = mapper.readTree(response.body()).path("choices").path(0).path("message").path("content");
            if (!content.isTextual() || content.asText().isBlank()) {
                log.warn("Veterinary AI response did not contain textual choices.message.content");
                throw ApiException.badGateway("El servicio veterinario devolvió una respuesta no válida.");
            }
            return content.asText();
        } catch (ApiException exception) {
            log.warn("Veterinary AI request ended with status={} message={}", exception.status().value(), exception.getMessage());
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Veterinary AI request interrupted exception={}", exception.getClass().getName());
            throw ApiException.serviceUnavailable("El servicio veterinario no está disponible en este momento.");
        } catch (HttpTimeoutException exception) {
            log.warn("Veterinary AI request timed out category=provider-timeout timeoutSeconds={} exception={}",
                    Math.max(1, properties.getReadTimeoutSeconds()), exception.getClass().getName());
            throw ApiException.serviceUnavailable("El servicio veterinario no está disponible en este momento.");
        } catch (IOException | RuntimeException exception) {
            log.warn("Veterinary AI request failed exception={} message={}", exception.getClass().getName(), safeExceptionMessage(exception));
            throw ApiException.serviceUnavailable("El servicio veterinario no está disponible en este momento.");
        }
    }

    private static HttpClient createHttpClient(VeterinaryAiProperties properties) {
        return HttpClient.newBuilder()
                // Evita negociaciones HTTP/2 que pueden quedar abiertas en algunas redes Windows.
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(Math.max(100, properties.getConnectTimeoutMs())))
                .build();
    }

    private HttpResponse<String> sendWithRetry(HttpRequest request) throws IOException, InterruptedException {
        int maxRetries = Math.max(0, Math.min(3, properties.getMaxRetries()));
        for (int attempt = 0; ; attempt++) {
            HttpResponse<String> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (HttpTimeoutException exception) {
                if (attempt >= maxRetries) throw exception;
                long delayMs = 750L * (attempt + 1);
                log.warn("Veterinary AI transient timeout retryAttempt={} retryDelayMs={}", attempt + 1, delayMs);
                Thread.sleep(delayMs);
                continue;
            }
            boolean transientFailure = response.statusCode() == 429 || response.statusCode() == 503;
            if (!transientFailure || attempt >= maxRetries) return response;
            long delayMs = 750L * (attempt + 1);
            log.warn("Veterinary AI transient response status={} retryAttempt={} retryDelayMs={}",
                    response.statusCode(), attempt + 1, delayMs);
            Thread.sleep(delayMs);
        }
    }

    private String safeEndpoint() {
        try {
            URI endpoint = URI.create(properties.getBaseUrl());
            return endpoint.getScheme() + "://" + endpoint.getHost() + endpoint.getPath();
        } catch (RuntimeException exception) {
            return "invalid-url";
        }
    }

    private boolean isGroqEndpoint() {
        try {
            return "api.groq.com".equalsIgnoreCase(URI.create(properties.getBaseUrl()).getHost());
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private ProviderError parseProviderError(int responseStatus, String responseBody) {
        if (responseBody == null || responseBody.isBlank()) return new ProviderError("none", "none", "none", "empty-response");
        try {
            JsonNode root = mapper.readTree(responseBody);
            String code = safeProviderValue(findField(root, "code"), "none", false);
            String status = safeProviderValue(findField(root, "status"), "none", false);
            JsonNode messageNode = findField(root, "message");
            if (messageNode == null) messageNode = findField(root, "detail");
            if (messageNode == null) messageNode = findField(root, "error_description");
            if (messageNode == null && root.path("error").isTextual()) messageNode = root.path("error");
            String message = safeProviderValue(messageNode, safeProviderBody(responseBody), true);
            return new ProviderError(code, status, message, providerCategory(responseStatus, status));
        } catch (IOException | RuntimeException exception) {
            return new ProviderError("unparseable", "unparseable", "unparseable", "unparseable-response");
        }
    }

    private JsonNode findField(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        if (node.isObject()) {
            JsonNode direct = node.get(field);
            if (direct != null && direct.isValueNode()) return direct;
            var fields = node.fields();
            while (fields.hasNext()) {
                JsonNode found = findField(fields.next().getValue(), field);
                if (found != null) return found;
            }
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                JsonNode found = findField(item, field);
                if (found != null) return found;
            }
        }
        return null;
    }

    private String safeProviderBody(String responseBody) {
        String safe = responseBody.replaceAll("(?i)(authorization|bearer|api[-_ ]?key|token)[=: ]+[^,;\\s}]*", "$1=[redacted]")
                .replaceAll("[\\r\\n\\t]+", " ").trim();
        if (safe.isBlank()) return "unrecognized-provider-error";
        return safe.substring(0, Math.min(240, safe.length()));
    }

    private String safeProviderValue(JsonNode value, String fallback, boolean message) {
        if (value == null || !value.isValueNode()) return fallback;
        String safe = value.asText("").replaceAll("(?i)(authorization|bearer|api[-_ ]?key|token)[=: ]+[^,; ]*", "$1=[redacted]")
                .replaceAll("[\\r\\n\\t]", " ").trim();
        if (safe.isBlank()) return fallback;
        if (!message) safe = safe.replaceAll("[^A-Za-z0-9_.:-]", "");
        return safe.substring(0, Math.min(message ? 240 : 80, safe.length()));
    }

    private String providerCategory(int responseStatus, String status) {
        if (responseStatus == 401 || responseStatus == 403) return "authentication-or-authorization";
        if (responseStatus == 404) return "endpoint-or-model-not-found";
        if (responseStatus == 429) return "rate-limit-or-quota";
        if (responseStatus == 503) return "provider-unavailable";
        return switch (status) {
            case "UNAUTHENTICATED", "PERMISSION_DENIED" -> "authentication-or-authorization";
            case "NOT_FOUND" -> "endpoint-or-model-not-found";
            case "RESOURCE_EXHAUSTED" -> "rate-limit-or-quota";
            case "UNAVAILABLE" -> "provider-unavailable";
            default -> "provider-error";
        };
    }

    private record ProviderError(String code, String status, String message, String category) { }

    private String safeExceptionMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) return "none";
        String safe = message.replaceAll("(?i)(authorization|bearer|api[-_ ]?key)[^,; ]*", "$1=[redacted]")
                .replaceAll("[\\r\\n\\t]", " ");
        return safe.substring(0, Math.min(160, safe.length()));
    }

    private String systemInstruction() {
        return "Eres un asistente de orientación veterinaria preventiva. No diagnostiques de forma definitiva, no prescribas medicamentos y no recomiendes tratamientos caseros peligrosos. "
                + "Clasifica la urgencia solo como GREEN, YELLOW o RED. RED significa atención veterinaria urgente. "
                + "Devuelve únicamente JSON válido, sin markdown, con exactamente estos campos: "
                + "urgency (GREEN|YELLOW|RED), summary (string), reasons (array de strings), warning_signs (array de strings), guidance (array de strings). "
                + "Incluye siempre que la orientación es preliminar y no sustituye a un veterinario. Si hay señales de emergencia, indica acudir inmediatamente a urgencias.";
    }
}
