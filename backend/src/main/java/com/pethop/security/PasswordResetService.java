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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder passwords;
    private final PasswordResetProperties properties;
    private final TransactionalEmailSender emailSender;

    public PasswordResetService(UserRepository users, PasswordResetTokenRepository tokens, PasswordEncoder passwords,
            PasswordResetProperties properties, TransactionalEmailSender emailSender) {
        this.users = users; this.tokens = tokens; this.passwords = passwords; this.properties = properties; this.emailSender = emailSender;
    }

    @Transactional
    public void request(String email) {
        if (!properties.isEnabled() || !properties.isMailEnabled()) {
            throw ApiException.serviceUnavailable("La recuperación de contraseña no está disponible en este momento.");
        }
        User user = users.findByEmail(normalize(email)).orElse(null);
        if (user == null) return;
        if (!emailSender.isConfigured()) throw ApiException.serviceUnavailable("El servicio de recuperación no está disponible.");
        Instant now = Instant.now();
        tokens.invalidateForUser(user.getId(), now);
        String rawToken = randomToken();
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user); token.setTokenHash(hash(rawToken));
        token.setExpiresAt(now.plusSeconds(Math.max(10, properties.getTokenTtlMinutes()) * 60));
        tokens.save(token);
        send(user.getEmail(), rawToken);
    }

    @Transactional
    public void confirm(String rawToken, String newPassword) {
        if (!properties.isEnabled() || !properties.isMailEnabled()) {
            throw ApiException.serviceUnavailable("La recuperación de contraseña no está disponible en este momento.");
        }
        PasswordResetToken token = tokens.findActive(hash(rawToken), Instant.now())
                .orElseThrow(() -> ApiException.badRequest("El enlace de recuperación no es válido o ya expiró."));
        User user = token.getUser();
        if (passwords.matches(newPassword, user.getPasswordHash())) throw ApiException.badRequest("La contraseña nueva debe ser distinta a la anterior.");
        user.setPasswordHash(passwords.encode(newPassword));
        token.setUsedAt(Instant.now());
        tokens.save(token); users.save(user);
    }

    private void send(String email, String token) {
        String base = properties.getFrontendUrl().replaceAll("/+$", "");
        String text = "Recibimos una solicitud para cambiar tu contraseña de PetHop.\n\n"
                + "Abre este enlace dentro de " + properties.getTokenTtlMinutes() + " minutos:\n"
                + base + "/acceso.html?mode=reset&token=" + token + "\n\n"
                + "Si no solicitaste este cambio, puedes ignorar este correo.";
        try { emailSender.send(email, "Restablece tu contraseña de PetHop", text); }
        catch (RuntimeException ex) { throw ApiException.serviceUnavailable("No fue posible enviar el correo de recuperación."); }
    }

    private String randomToken() { byte[] value = new byte[32]; RANDOM.nextBytes(value); return Base64.getUrlEncoder().withoutPadding().encodeToString(value); }
    private String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    private String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 no disponible", ex); }
    }
}
