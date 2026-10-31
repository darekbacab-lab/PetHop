package com.pethop.adoption;
import com.pethop.adoption.dto.AdoptionResponse;
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
@Validated @RestController @RequestMapping("/api/v1/shelters/me/adoptions")
public class ShelterAdoptionController {
    private final AdoptionService service;public ShelterAdoptionController(AdoptionService service){this.service=service;}
    @GetMapping PageResponse<AdoptionResponse> list(@AuthenticationPrincipal SecurityUser user,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="20") @Min(1) @Max(50) int size){return service.shelter(user,page,size);}
}
