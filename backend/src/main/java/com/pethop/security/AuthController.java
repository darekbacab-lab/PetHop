package com.pethop.security;

import com.pethop.security.dto.LoginRequest;
import com.pethop.security.dto.RegisterRequest;
import com.pethop.security.dto.PasswordResetConfirmRequest;
import com.pethop.security.dto.PasswordResetRequest;
import com.pethop.common.exception.ApiException;
import com.pethop.user.dto.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository;
    private final SessionAuthenticationStrategy sessionStrategy;
    private final PasswordResetService passwordReset;
    private final EmailVerificationService emailVerification;
    private final EmailVerificationProperties emailVerificationProperties;

    public AuthController(AuthService authService, AuthenticationManager authenticationManager,
            SecurityContextRepository contextRepository, SessionAuthenticationStrategy sessionStrategy,
            PasswordResetService passwordReset, EmailVerificationService emailVerification, EmailVerificationProperties emailVerificationProperties) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.contextRepository = contextRepository;
        this.sessionStrategy = sessionStrategy;
        this.passwordReset = passwordReset; this.emailVerification = emailVerification; this.emailVerificationProperties = emailVerificationProperties;
    }

    @GetMapping("/csrf")
    CsrfToken csrf(CsrfToken token) { return token; }

    @PostMapping("/register")
    ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        var created = authService.register(request);
        emailVerification.issue(created);
        var location = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/v1/users/me").build().toUri();
        return ResponseEntity.created(location).body(UserResponse.from(created));
    }

    @PostMapping("/login")
    UserResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.email().trim().toLowerCase(), request.password()));
        if (emailVerificationProperties.isRequired() && !((SecurityUser) authentication.getPrincipal()).user().isEmailVerified())
            throw ApiException.forbidden("Confirma tu correo electrónico antes de iniciar sesión.");
        sessionStrategy.onAuthentication(authentication, servletRequest, response);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, servletRequest, response);
        return UserResponse.from(((SecurityUser) authentication.getPrincipal()).user());
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset/request")
    ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        passwordReset.request(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/password-reset/confirm")
    ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordReset.confirm(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/email-verification/confirm")
    ResponseEntity<Void> confirmEmail(@RequestBody java.util.Map<String, String> request) { emailVerification.confirm(request.get("token")); return ResponseEntity.noContent().build(); }

    @GetMapping("/me")
    UserResponse me(@AuthenticationPrincipal SecurityUser principal) { return UserResponse.from(principal.user()); }
}
