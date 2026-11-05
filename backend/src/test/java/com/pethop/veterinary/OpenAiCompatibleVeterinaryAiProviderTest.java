package com.pethop.veterinary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pethop.common.exception.ApiException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OpenAiCompatibleVeterinaryAiProviderTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private VeterinaryAiProperties properties;
    private HttpClient httpClient;
    private HttpResponse<String> httpResponse;

    @BeforeEach
    void setUp() {
        properties = new VeterinaryAiProperties();
        properties.setEnabled(true); properties.setApiKey("test-key"); properties.setModel("gemini-3.8-flash");
        properties.setBaseUrl("https://generativelanguage.googleapis.com/v1beta/openai/chat/completions");
        httpClient = mock(HttpClient.class);
        httpResponse = mock(HttpResponse.class);
    }

    @Test
    void sendsGeminiCompatibleChatRequestAndReadsOpenAiResponse() throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"choices\":[{\"message\":{\"content\":\"{\\\"urgency\\\":\\\"GREEN\\\"}\"}}]}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        String response = new OpenAiCompatibleVeterinaryAiProvider(mapper, properties, httpClient).generate("observación de prueba");

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        org.mockito.Mockito.verify(httpClient).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));
        assertThat(response).contains("GREEN");
        assertThat(requestCaptor.getValue().uri().toString()).isEqualTo(properties.getBaseUrl());
        assertThat(requestCaptor.getValue().headers().firstValue("Authorization")).hasValue("Bearer test-key");
        assertThat(requestCaptor.getValue().headers().firstValue("Content-Type")).hasValue("application/json");
        assertThat(requestCaptor.getValue().bodyPublisher()).isPresent();
    }

    @Test
    void convertsProviderHttpFailureToSafeUnavailableError() throws Exception {
        when(httpResponse.statusCode()).thenReturn(401);
        when(httpResponse.body()).thenReturn("{\"error\":{\"code\":\"invalid_api_key\",\"message\":\"API key rejected by provider\"}}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        assertThatThrownBy(() -> new OpenAiCompatibleVeterinaryAiProvider(mapper, properties, httpClient).generate("private observation"))
                .isInstanceOf(ApiException.class)
                .hasMessage("El servicio veterinario no está disponible en este momento.");
    }

    @Test
    void recordsGemini503ErrorFieldsWithoutExposingThemPublicly() throws Exception {
        when(httpResponse.statusCode()).thenReturn(503);
        when(httpResponse.body()).thenReturn("{\"error\":{\"code\":503,\"message\":\"Service temporarily unavailable\",\"status\":\"UNAVAILABLE\"}}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        assertThatThrownBy(() -> new OpenAiCompatibleVeterinaryAiProvider(mapper, properties, httpClient).generate("question"))
                .isInstanceOf(ApiException.class)
                .hasMessage("El servicio veterinario no está disponible en este momento.")
                .hasNoCause();
    }

    @Test
    void readsProviderMessageFromAlternateErrorShape() throws Exception {
        when(httpResponse.statusCode()).thenReturn(503);
        when(httpResponse.body()).thenReturn("{\"error\":\"upstream temporarily unavailable\",\"detail\":\"retry later\"}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        assertThatThrownBy(() -> new OpenAiCompatibleVeterinaryAiProvider(mapper, properties, httpClient).generate("question"))
                .isInstanceOf(ApiException.class)
                .hasMessage("El servicio veterinario no está disponible en este momento.");
    }

    @Test
    void retriesTemporaryProviderOverloadOnceAndUsesSuccessfulResponse() throws Exception {
        properties.setMaxRetries(1);
        HttpResponse<String> unavailable = mock(HttpResponse.class);
        when(unavailable.statusCode()).thenReturn(503);
        when(unavailable.body()).thenReturn("{\"error\":{\"code\":503,\"status\":\"UNAVAILABLE\",\"message\":\"high demand\"}}");
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(unavailable, httpResponse);

        assertThat(new OpenAiCompatibleVeterinaryAiProvider(mapper, properties, httpClient).generate("question"))
                .isEqualTo("ok");
        org.mockito.Mockito.verify(httpClient, org.mockito.Mockito.times(2))
                .send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void retriesOneTimeoutAndUsesSuccessfulResponse() throws Exception {
        properties.setMaxRetries(1);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new HttpTimeoutException("request timed out"))
                .thenReturn(httpResponse);

        assertThat(new OpenAiCompatibleVeterinaryAiProvider(mapper, properties, httpClient).generate("question"))
                .isEqualTo("ok");
        org.mockito.Mockito.verify(httpClient, org.mockito.Mockito.times(2))
                .send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void doesNotCallProviderWhenDisabledOrMissingKey() {
        properties.setEnabled(false);

        assertThatThrownBy(() -> new OpenAiCompatibleVeterinaryAiProvider(mapper, properties, httpClient).generate("question"))
                .isInstanceOf(ApiException.class)
                .hasMessage("El asistente veterinario no está disponible en este momento.");
    }
}
