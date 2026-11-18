package com.pethop.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pethop.config.SecurityConfig;
import com.pethop.user.User;
import com.pethop.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.UUID;

@WebMvcTest(controllers = AuthController.class)
@Import(SecurityConfig.class)
class AuthSecurityRegressionTest {
    @Autowired MockMvc mvc;
    @MockitoBean AuthService authService;
    @MockitoBean AuthenticationManager authenticationManager;
    @MockitoBean SecurityUserService securityUserService;
    @MockitoBean PasswordResetService passwordResetService;
    @MockitoBean EmailVerificationService emailVerificationService;

    @Test
    void missingSessionReturnsUnauthorizedInsteadOfForbidden() throws Exception {
        mvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedSessionCanReadCurrentUser() throws Exception {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.setFullName("Adoptante");
        user.setEmail("adoptante@example.test");
        user.setRole(UserRole.ADOPTER);

        mvc.perform(get("/api/v1/auth/me").with(SecurityMockMvcRequestPostProcessors.user(new SecurityUser(user))))
                .andExpect(status().isOk());
    }

    @Test
    void emailVerificationResendIsPublicAndAccepted() throws Exception {
        mvc.perform(post("/api/v1/auth/email-verification/resend")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"adoptante@example.test\"}"))
                .andExpect(status().isAccepted());
        verify(emailVerificationService).resend("adoptante@example.test");
    }

    @Test
    void honeypotFilledRegistrationIsRejectedBeforeCreatingTheAccount() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Bot\",\"email\":\"bot@example.test\",\"password\":\"123456789012\",\"phone\":\"\",\"role\":\"ADOPTER\",\"website\":\"http://spam.example\"}"))
                .andExpect(status().isBadRequest());
        verify(authService, never()).register(any());
    }

    @Test
    void honeypotFilledPasswordResetIsSilentlyIgnored() throws Exception {
        mvc.perform(post("/api/v1/auth/password-reset/request")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"adoptante@example.test\",\"website\":\"http://spam.example\"}"))
                .andExpect(status().isAccepted());
        verify(passwordResetService, never()).request(anyString());
    }

    @Test
    void honeypotFilledVerificationResendIsSilentlyIgnored() throws Exception {
        mvc.perform(post("/api/v1/auth/email-verification/resend")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"adoptante@example.test\",\"website\":\"http://spam.example\"}"))
                .andExpect(status().isAccepted());
        verify(emailVerificationService, never()).resend(anyString());
    }

    @Test
    void emptyHoneypotPasswordResetStillSendsTheEmail() throws Exception {
        mvc.perform(post("/api/v1/auth/password-reset/request")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"adoptante@example.test\",\"website\":\"\"}"))
                .andExpect(status().isAccepted());
        verify(passwordResetService).request("adoptante@example.test");
    }
}
