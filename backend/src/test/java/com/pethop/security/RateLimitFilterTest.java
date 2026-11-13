package com.pethop.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import jakarta.servlet.FilterChain;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {
    @Test
    void returns429ForLoginAfterConfiguredLimit() throws Exception {
        RateLimitProperties properties = properties(2, 60);
        RateLimitFilter filter = new RateLimitFilter(new InMemoryRateLimiter(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)), properties);
        FilterChain chain = mock(FilterChain.class);

        for (int attempt = 0; attempt < 3; attempt++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request("/api/v1/auth/login", "POST"), response, chain);
            if (attempt == 2) {
                assertThat(response.getStatus()).isEqualTo(429);
                assertThat(response.getHeader("Retry-After")).isEqualTo("60");
            }
        }
        verify(chain, times(2)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void limitsUploadsAndReportsWithoutChangingOtherEndpoints() throws Exception {
        RateLimitProperties properties = properties(1, 60);
        RateLimitFilter filter = new RateLimitFilter(new InMemoryRateLimiter(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)), properties);
        AtomicInteger passed = new AtomicInteger();
        FilterChain chain = (request, response) -> passed.incrementAndGet();

        filter.doFilter(request("/api/v1/pets/1/images", "POST"), new MockHttpServletResponse(), chain);
        MockHttpServletResponse blockedUpload = new MockHttpServletResponse();
        filter.doFilter(request("/api/v1/pets/1/images", "POST"), blockedUpload, chain);
        assertThat(blockedUpload.getStatus()).isEqualTo(429);

        filter.doFilter(request("/api/v1/pets/1/reports", "POST"), new MockHttpServletResponse(), chain);
        MockHttpServletResponse blockedReport = new MockHttpServletResponse();
        filter.doFilter(request("/api/v1/pets/1/reports", "POST"), blockedReport, chain);
        assertThat(blockedReport.getStatus()).isEqualTo(429);

        filter.doFilter(request("/api/v1/pets", "GET"), new MockHttpServletResponse(), chain);
        assertThat(passed.get()).isEqualTo(3);
    }

    @Test
    void limitsVeterinaryAssistantMessages() throws Exception {
        RateLimitProperties properties = properties(1, 60);
        RateLimitFilter filter = new RateLimitFilter(new InMemoryRateLimiter(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)), properties);
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request("/api/v1/pets/1/veterinary-assistant/messages", "POST"), new MockHttpServletResponse(), chain);
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(request("/api/v1/pets/1/veterinary-assistant/messages", "POST"), blocked, chain);
        assertThat(blocked.getStatus()).isEqualTo(429);
    }

    @Test
    void limitsEmailVerificationResendRequests() throws Exception {
        RateLimitProperties properties = properties(5, 3600);
        properties.setEmailVerificationResend(new RateLimitProperties.Policy(1, 60));
        RateLimitFilter filter = new RateLimitFilter(new InMemoryRateLimiter(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)), properties);
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request("/api/v1/auth/email-verification/resend", "POST"), new MockHttpServletResponse(), chain);
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(request("/api/v1/auth/email-verification/resend", "POST"), blocked, chain);
        assertThat(blocked.getStatus()).isEqualTo(429);
    }

    private RateLimitProperties properties(int maxRequests, long windowSeconds) {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setLogin(new RateLimitProperties.Policy(maxRequests, windowSeconds));
        properties.setRegistration(new RateLimitProperties.Policy(maxRequests, windowSeconds));
        properties.setUploads(new RateLimitProperties.Policy(maxRequests, windowSeconds));
        properties.setReports(new RateLimitProperties.Policy(maxRequests, windowSeconds));
        properties.setVeterinaryAssistant(new RateLimitProperties.Policy(maxRequests, windowSeconds));
        return properties;
    }

    private MockHttpServletRequest request(String uri, String method) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr("192.0.2.10");
        return request;
    }
}
