package com.pethop.adoption;

import com.pethop.adoption.dto.ApplicationReviewResponse;
import com.pethop.common.response.PageResponse;
import com.pethop.security.SecurityUser;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated @RestController @RequestMapping("/api/v1/shelters/me/applications")
public class ShelterApplicationController {
    private final ApplicationService service;
    public ShelterApplicationController(ApplicationService service){this.service=service;}
    @GetMapping PageResponse<ApplicationReviewResponse> list(@AuthenticationPrincipal SecurityUser user,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="20") @Min(1) @Max(50) int size){return service.received(user,page,size);}
}
