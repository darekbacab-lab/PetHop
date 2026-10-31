package com.pethop.adoption;

import com.pethop.adoption.dto.AdoptionResponse;
import com.pethop.adoption.dto.CreateAdoptionRequest;
import com.pethop.common.response.PageResponse;
import com.pethop.security.SecurityUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated @RestController @RequestMapping("/api/v1/adoptions")
public class AdoptionController {
    private final AdoptionService service;public AdoptionController(AdoptionService service){this.service=service;}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) AdoptionResponse create(@AuthenticationPrincipal SecurityUser user,@Valid @RequestBody CreateAdoptionRequest request){return service.create(user,request);}
    @GetMapping("/mine") PageResponse<AdoptionResponse> mine(@AuthenticationPrincipal SecurityUser user,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="20") @Min(1) @Max(50) int size){return service.mine(user,page,size);}
    @GetMapping("/{id}") AdoptionResponse get(@PathVariable UUID id,@AuthenticationPrincipal SecurityUser user){return AdoptionResponse.from(service.authorized(id,user));}
}
