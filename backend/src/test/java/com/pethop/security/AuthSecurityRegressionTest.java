package com.pethop.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pethop.config.SecurityConfig;
import com.pethop.user.User;
import com.pethop.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
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
}
