package com.pethop.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pethop.password-reset")
public class PasswordResetProperties {
    private boolean enabled;
    private boolean mailEnabled;
    private String frontendUrl = "http://localhost:5173";
    private long tokenTtlMinutes = 30;
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isMailEnabled() { return mailEnabled; }
    public void setMailEnabled(boolean mailEnabled) { this.mailEnabled = mailEnabled; }
    public String getFrontendUrl() { return frontendUrl; }
    public void setFrontendUrl(String frontendUrl) { this.frontendUrl = frontendUrl; }
    public long getTokenTtlMinutes() { return tokenTtlMinutes; }
    public void setTokenTtlMinutes(long tokenTtlMinutes) { this.tokenTtlMinutes = tokenTtlMinutes; }
}
