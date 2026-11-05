package com.pethop.veterinary;

import com.pethop.security.SecurityUser;
import com.pethop.veterinary.dto.VeterinaryAssistantMessageRequest;
import com.pethop.veterinary.dto.VeterinaryAssistantResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/veterinary-assistant")
public class GeneralVeterinaryAssistantController {
    private final VeterinaryAssistantService service;
    public GeneralVeterinaryAssistantController(VeterinaryAssistantService service) { this.service = service; }

    @PostMapping("/messages")
    public VeterinaryAssistantResponse message(@AuthenticationPrincipal SecurityUser user,
            @Valid @RequestBody VeterinaryAssistantMessageRequest request) {
        return service.askGeneral(user, request.petId(), request.message());
    }
}
