package com.pethop.user;

import com.pethop.security.AuthService;
import com.pethop.security.SecurityUser;
import com.pethop.user.dto.ChangePasswordRequest;
import com.pethop.user.dto.UpdateProfileRequest;
import com.pethop.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserController {
    private final AuthService auth;
    public UserController(AuthService auth) { this.auth = auth; }
    @GetMapping UserResponse get(@AuthenticationPrincipal SecurityUser user) { return UserResponse.from(user.user()); }
    @PatchMapping UserResponse update(@AuthenticationPrincipal SecurityUser user, @Valid @RequestBody UpdateProfileRequest request) {
        return UserResponse.from(auth.updateProfile(user.user(), request));
    }
    @PatchMapping("/password")
    org.springframework.http.ResponseEntity<Void> changePassword(@AuthenticationPrincipal SecurityUser user,
            @Valid @RequestBody ChangePasswordRequest request) {
        auth.changePassword(user.user(), request);
        return org.springframework.http.ResponseEntity.noContent().build();
    }
}
