package com.pethop.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pethop.rate-limit")
public class RateLimitProperties {
    private boolean enabled = true;
    private boolean trustForwardedHeaders;
    private Policy login = new Policy(10, 60);
    private Policy registration = new Policy(5, 3600);
    private Policy uploads = new Policy(30, 60);
    private Policy reports = new Policy(10, 3600);
    private Policy veterinaryAssistant = new Policy(20, 3600);

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isTrustForwardedHeaders() { return trustForwardedHeaders; }
    public void setTrustForwardedHeaders(boolean trustForwardedHeaders) { this.trustForwardedHeaders = trustForwardedHeaders; }
    public Policy getLogin() { return login; }
    public void setLogin(Policy login) { this.login = login; }
    public Policy getRegistration() { return registration; }
    public void setRegistration(Policy registration) { this.registration = registration; }
    public Policy getUploads() { return uploads; }
    public void setUploads(Policy uploads) { this.uploads = uploads; }
    public Policy getReports() { return reports; }
    public void setReports(Policy reports) { this.reports = reports; }
    public Policy getVeterinaryAssistant() { return veterinaryAssistant; }
    public void setVeterinaryAssistant(Policy veterinaryAssistant) { this.veterinaryAssistant = veterinaryAssistant; }

    public static class Policy {
        private int maxRequests;
        private long windowSeconds;
        public Policy() { }
        public Policy(int maxRequests, long windowSeconds) { this.maxRequests = maxRequests; this.windowSeconds = windowSeconds; }
        public int getMaxRequests() { return maxRequests; }
        public void setMaxRequests(int maxRequests) { this.maxRequests = maxRequests; }
        public long getWindowSeconds() { return windowSeconds; }
        public void setWindowSeconds(long windowSeconds) { this.windowSeconds = windowSeconds; }
    }
}
