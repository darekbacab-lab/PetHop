package com.pethop.pet;

import com.pethop.common.response.PageResponse;
import com.pethop.pet.dto.ModerationDecisionRequest;
import com.pethop.pet.dto.ModerationHistoryResponse;
import com.pethop.pet.dto.PetResponse;
import com.pethop.security.SecurityUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/admin/pets")
public class PetModerationController {
    private final PetModerationService service;
    public PetModerationController(PetModerationService service) { this.service = service; }
    @GetMapping("/pending") PageResponse<PetResponse> pending(@RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) { return service.pending(page, size); }
    @GetMapping("/{id}") PetResponse get(@PathVariable UUID id) { return service.get(id); }
    @PatchMapping("/{id}/moderation") PetResponse decide(@PathVariable UUID id, @AuthenticationPrincipal SecurityUser admin,
            @Valid @RequestBody ModerationDecisionRequest request) { return service.decide(id, admin, request); }
    @GetMapping("/{id}/moderation-history") List<ModerationHistoryResponse> history(@PathVariable UUID id) { return service.history(id); }
}
