package com.pethop.veterinary;

import com.pethop.security.SecurityUser;
import com.pethop.veterinary.dto.VeterinaryAssistantMessageRequest;
import com.pethop.veterinary.dto.VeterinaryAssistantResponse;
import com.pethop.veterinary.dto.VeterinaryPetContextResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pets/{petId}/veterinary-assistant")
public class VeterinaryAssistantController {
    private final VeterinaryAssistantService service;
    public VeterinaryAssistantController(VeterinaryAssistantService service) { this.service = service; }

    @GetMapping("/context")
    public VeterinaryPetContextResponse context(@AuthenticationPrincipal SecurityUser user, @PathVariable UUID petId) {
        return service.context(user, petId);
    }

    @PostMapping("/messages")
    public VeterinaryAssistantResponse message(@AuthenticationPrincipal SecurityUser user, @PathVariable UUID petId,
            @Valid @RequestBody VeterinaryAssistantMessageRequest request) {
        return service.ask(user, petId, request.message());
    }
}
