package com.pethop.veterinary;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pethop.veterinary-ai")
public class VeterinaryAiProperties {
    private boolean enabled;
    private String apiKey = "";
    private String baseUrl = "https://api.openai.com/v1/chat/completions";
    private String model = "gpt-4o-mini";
    private int connectTimeoutMs = 5000;
    private long readTimeoutSeconds = 30;
    private int maxOutputTokens = 700;
    private int maxRetries = 1;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
    public long getReadTimeoutSeconds() { return readTimeoutSeconds; }
    public void setReadTimeoutSeconds(long readTimeoutSeconds) { this.readTimeoutSeconds = readTimeoutSeconds; }
    public int getMaxOutputTokens() { return maxOutputTokens; }
    public void setMaxOutputTokens(int maxOutputTokens) { this.maxOutputTokens = maxOutputTokens; }
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
}
