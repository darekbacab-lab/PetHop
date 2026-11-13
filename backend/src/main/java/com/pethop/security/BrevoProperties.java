package com.pethop.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pethop.brevo")
public class BrevoProperties {
    private String apiKey;
    private String from = "no-reply@pethop.local";
    private String apiUrl = "https://api.brevo.com/v3/smtp/email";

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
}
