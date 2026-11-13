package com.pethop.security;

import com.pethop.common.exception.ApiException;
import com.pethop.user.User;
import com.pethop.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailVerificationService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final EmailVerificationTokenRepository tokens;
    private final UserRepository users;
    private final EmailVerificationProperties properties;
    private final TransactionalEmailSender emailSender;

    public EmailVerificationService(EmailVerificationTokenRepository tokens, UserRepository users,
            EmailVerificationProperties properties, TransactionalEmailSender emailSender) {
        this.tokens = tokens; this.users = users; this.properties = properties; this.emailSender = emailSender;
    }

    @Transactional
    public void issue(User user) {
        if (!properties.isEnabled() || !properties.isMailEnabled()) return;
        if (!emailSender.isConfigured()) throw ApiException.serviceUnavailable("No fue posible enviar el correo de verificación.");
        Instant now = Instant.now();
        tokens.invalidateForUser(user.getId(), now);
        String raw = random();
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setTokenHash(hash(raw));
        token.setExpiresAt(now.plusSeconds(Math.max(10, properties.getTokenTtlMinutes()) * 60));
        tokens.save(token);
        String link = properties.getFrontendUrl().replaceAll("/+$", "") + "/acceso.html?mode=verify&token=" + raw;
        try {
            emailSender.send(user.getEmail(), "Confirma tu correo de PetHop",
                    "Confirma tu correo dentro de " + properties.getTokenTtlMinutes() + " minutos:\n\n" + link);
        } catch (RuntimeException ex) {
            throw ApiException.serviceUnavailable("No fue posible enviar el correo de verificación.");
        }
    }

    /**
     * Reenvía el correo de verificación. Responde igual exista o no la cuenta, y tanto si ya está
     * verificada como si no, para no filtrar qué correos están registrados.
     */
    @Transactional
    public void resend(String email) {
        if (!properties.isEnabled() || !properties.isMailEnabled()) {
            throw ApiException.serviceUnavailable("La verificación de correo no está disponible en este momento.");
        }
        User user = users.findByEmail(normalize(email)).orElse(null);
        if (user == null || user.isEmailVerified()) return;
        issue(user);
    }

    @Transactional
    public void confirm(String raw) {
        if (!properties.isEnabled()) throw ApiException.serviceUnavailable("La verificación de correo no está disponible.");
        if (raw == null || raw.isBlank()) throw ApiException.badRequest("El enlace de verificación no es válido.");
        EmailVerificationToken token = tokens.findActive(hash(raw), Instant.now())
                .orElseThrow(() -> ApiException.badRequest("El enlace de verificación no es válido o expiró."));
        User user = token.getUser();
        user.setEmailVerified(true);
        token.setUsedAt(Instant.now());
        users.save(user);
        tokens.save(token);
    }

    private String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }

    private String random() {
        byte[] value = new byte[32];
        RANDOM.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
