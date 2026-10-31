package com.pethop.security;

import com.pethop.common.exception.ApiException;
import com.pethop.security.dto.RegisterRequest;
import com.pethop.user.AccountStatus;
import com.pethop.user.User;
import com.pethop.user.UserRepository;
import com.pethop.user.UserRole;
import com.pethop.user.dto.ChangePasswordRequest;
import com.pethop.user.dto.UpdateProfileRequest;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwords;

    public AuthService(UserRepository users, PasswordEncoder passwords) {
        this.users = users;
        this.passwords = passwords;
    }

    @Transactional
    public User register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (users.existsByEmail(email)) throw ApiException.conflict("Ya existe una cuenta con ese correo.");
        UserRole role = request.role() == null ? UserRole.ADOPTER : request.role();
        if (role == UserRole.ADMIN) throw ApiException.forbidden("No es posible crear una cuenta administradora desde el registro público.");
        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwords.encode(request.password()));
        user.setPhone(request.phone() == null || request.phone().isBlank() ? null : request.phone().trim());
        user.setRole(role);
        user.setAccountStatus(AccountStatus.ACTIVE);
        return users.save(user);
    }

    @Transactional
    public User updateProfile(User user, UpdateProfileRequest request) {
        if (request.fullName() != null) user.setFullName(request.fullName().trim());
        if (request.phone() != null) user.setPhone(request.phone().isBlank() ? null : request.phone().trim());
        return users.save(user);
    }

    public void changePassword(User user, ChangePasswordRequest request) {
        if (!passwords.matches(request.currentPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("La contraseña actual no coincide.");
        }
        if (passwords.matches(request.newPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("La contraseña nueva debe ser distinta a la actual.");
        }
        user.setPasswordHash(passwords.encode(request.newPassword()));
        users.save(user);
    }

    private String normalizeEmail(String email) { return email.trim().toLowerCase(Locale.ROOT); }
}
