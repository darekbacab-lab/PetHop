package com.pethop.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pethop.email-verification")
public class EmailVerificationProperties {
    private boolean enabled;
    private boolean required;
    private boolean mailEnabled;
    private String frontendUrl = "http://localhost:5173";
    private int tokenTtlMinutes = 60;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    public boolean isMailEnabled() { return mailEnabled; }
    public void setMailEnabled(boolean mailEnabled) { this.mailEnabled = mailEnabled; }
    public String getFrontendUrl() { return frontendUrl; }
    public void setFrontendUrl(String frontendUrl) { this.frontendUrl = frontendUrl; }
    public int getTokenTtlMinutes() { return tokenTtlMinutes; }
    public void setTokenTtlMinutes(int tokenTtlMinutes) { this.tokenTtlMinutes = tokenTtlMinutes; }
}
