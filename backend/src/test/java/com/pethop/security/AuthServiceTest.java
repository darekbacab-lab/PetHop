package com.pethop.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.pethop.admin.AdminAuditService;
import com.pethop.common.exception.ApiException;
import com.pethop.security.dto.RegisterRequest;
import com.pethop.user.dto.ChangeRoleRequest;
import com.pethop.user.User;
import com.pethop.user.UserRepository;
import com.pethop.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserRepository users;
    @Mock PasswordEncoder passwords;
    @Mock AdminAuditService audit;
    @InjectMocks AuthService service;

    @Test
    void registrationNormalizesEmailAndNeverStoresThePassword() {
        when(users.existsByEmail("ana@example.com")).thenReturn(false);
        when(passwords.encode("una-clave-larga-123")).thenReturn("bcrypt-hash");
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = service.register(new RegisterRequest(" Ana López ", " ANA@EXAMPLE.COM ",
                "una-clave-larga-123", "+52 999 123 4567", UserRole.ADOPTER));

        assertEquals("ana@example.com", created.getEmail());
        assertEquals("Ana López", created.getFullName());
        assertEquals("bcrypt-hash", created.getPasswordHash());
        assertNotEquals("una-clave-larga-123", created.getPasswordHash());
    }

    @Test
    void publicRegistrationRejectsAdministratorRole() {
        when(users.existsByEmail("ana@example.com")).thenReturn(false);
        ApiException error = assertThrows(ApiException.class, () -> service.register(new RegisterRequest(
                "Ana", "ana@example.com", "una-clave-larga-123", null, UserRole.ADMIN)));
        assertEquals(HttpStatus.FORBIDDEN, error.status());
    }

    @Test
    void duplicateEmailIsRejected() {
        when(users.existsByEmail("ana@example.com")).thenReturn(true);
        ApiException error = assertThrows(ApiException.class, () -> service.register(new RegisterRequest(
                "Ana", "ana@example.com", "una-clave-larga-123", null, UserRole.ADOPTER)));
        assertEquals(HttpStatus.CONFLICT, error.status());
    }

    @Test
    void accountCanChangeBetweenAdopterAndShelterButNeverToAdministrator() {
        User user = new User();
        user.setRole(UserRole.ADOPTER);
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User changed = service.changeRole(user, new ChangeRoleRequest(UserRole.SHELTER));
        assertEquals(UserRole.SHELTER, changed.getRole());
        org.mockito.Mockito.verify(audit).record(changed, "USER_ROLE_CHANGED", "USER", changed.getId(), "ADOPTER", "SHELTER", null);

        ApiException error = assertThrows(ApiException.class, () -> service.changeRole(user, new ChangeRoleRequest(UserRole.ADMIN)));
        assertEquals(HttpStatus.FORBIDDEN, error.status());
    }
}
