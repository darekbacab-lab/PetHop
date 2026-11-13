package com.pethop.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class RateLimitFilter extends OncePerRequestFilter {
    private static final Pattern PET_UPLOAD = Pattern.compile("/api/v1/pets/[^/]+/images");
    private static final Pattern PET_REPORT = Pattern.compile("/api/v1/pets/[^/]+/reports");
    private static final Pattern FOLLOW_UP_UPLOAD = Pattern.compile("/api/v1/follow-ups/[^/]+/media");
    private static final Pattern VETERINARY_ASSISTANT = Pattern.compile("/api/v1/pets/[^/]+/veterinary-assistant(?:/.*)?");
    private static final Pattern GENERAL_VETERINARY_ASSISTANT = Pattern.compile("/api/v1/veterinary-assistant(?:/.*)?");
    private final InMemoryRateLimiter limiter;
    private final RateLimitProperties properties;

    public RateLimitFilter(InMemoryRateLimiter limiter, RateLimitProperties properties) {
        this.limiter = limiter;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!properties.isEnabled() || !"POST".equalsIgnoreCase(request.getMethod()) || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        Rule rule = ruleFor(request.getRequestURI());
        if (rule == null) { chain.doFilter(request, response); return; }
        String identity = identity(request, rule.ipOnly());
        String key = rule.name() + ":" + identity;
        if (!limiter.tryAcquire(key, rule.policy().getMaxRequests(), rule.policy().getWindowSeconds())) {
            long retryAfter = Math.max(1, rule.policy().getWindowSeconds());
            response.setStatus(429);
            response.setHeader("Retry-After", Long.toString(retryAfter));
            response.setContentType("application/problem+json;charset=UTF-8");
            response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"Too Many Requests\",\"status\":429,\"detail\":\"Demasiadas solicitudes. Intenta de nuevo más tarde.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private Rule ruleFor(String path) {
        if ("/api/v1/auth/login".equals(path)) return new Rule("login", properties.getLogin(), true);
        if ("/api/v1/auth/register".equals(path)) return new Rule("registration", properties.getRegistration(), true);
        if ("/api/v1/auth/password-reset/request".equals(path)) return new Rule("password-reset", properties.getPasswordReset(), true);
        if ("/api/v1/auth/email-verification/resend".equals(path)) return new Rule("email-verification-resend", properties.getEmailVerificationResend(), true);
        if (PET_UPLOAD.matcher(path).matches() || FOLLOW_UP_UPLOAD.matcher(path).matches()) return new Rule("uploads", properties.getUploads(), false);
        if (PET_REPORT.matcher(path).matches()) return new Rule("reports", properties.getReports(), false);
        if (VETERINARY_ASSISTANT.matcher(path).matches()) return new Rule("veterinary-assistant", properties.getVeterinaryAssistant(), false);
        if (GENERAL_VETERINARY_ASSISTANT.matcher(path).matches()) return new Rule("veterinary-assistant", properties.getVeterinaryAssistant(), false);
        return null;
    }

    private String identity(HttpServletRequest request, boolean ipOnly) {
        if (!ipOnly) {
            Object principal = SecurityContextHolder.getContext().getAuthentication() == null ? null
                    : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof SecurityUser user) return "user:" + user.id();
        }
        return "ip:" + clientAddress(request);
    }

    private String clientAddress(HttpServletRequest request) {
        if (properties.isTrustForwardedHeaders()) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",", 2)[0].trim().toLowerCase(Locale.ROOT);
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private record Rule(String name, RateLimitProperties.Policy policy, boolean ipOnly) { }
}
