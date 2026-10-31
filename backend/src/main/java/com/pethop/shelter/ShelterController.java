package com.pethop.shelter;

import com.pethop.security.SecurityUser;
import com.pethop.shelter.dto.ShelterRequest;
import com.pethop.shelter.dto.ShelterResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shelters")
public class ShelterController {
    private final ShelterService service;
    public ShelterController(ShelterService service) { this.service = service; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    ShelterResponse create(@AuthenticationPrincipal SecurityUser user, @Valid @RequestBody ShelterRequest request) {
        return ShelterResponse.from(service.create(user.user(), request));
    }
    @GetMapping("/me") ShelterResponse mine(@AuthenticationPrincipal SecurityUser user) { return ShelterResponse.from(service.mine(user.user())); }
    @PatchMapping("/me") ShelterResponse update(@AuthenticationPrincipal SecurityUser user, @Valid @RequestBody ShelterRequest request) {
        return ShelterResponse.from(service.update(user.user(), request));
    }
}
