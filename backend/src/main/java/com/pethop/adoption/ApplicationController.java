package com.pethop.adoption;

import com.pethop.adoption.dto.ApplicationResponse;
import com.pethop.adoption.dto.CreateApplicationRequest;
import com.pethop.adoption.dto.StatusHistoryResponse;
import com.pethop.adoption.dto.UpdateApplicationStatusRequest;
import com.pethop.common.response.PageResponse;
import com.pethop.security.SecurityUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated @RestController @RequestMapping("/api/v1/adoption-applications")
public class ApplicationController {
    private final ApplicationService service;
    public ApplicationController(ApplicationService service){this.service=service;}
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    ApplicationResponse create(@AuthenticationPrincipal SecurityUser user,@Valid @RequestBody CreateApplicationRequest request){return service.create(user,request);}
    @GetMapping("/mine") PageResponse<ApplicationResponse> mine(@AuthenticationPrincipal SecurityUser user,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="20") @Min(1) @Max(50) int size){return service.mine(user,page,size);}
    @GetMapping("/{id}") ApplicationResponse get(@AuthenticationPrincipal SecurityUser user,@PathVariable UUID id){return service.get(user,id);}
    @PatchMapping("/{id}/status") ResponseEntity<Void> changeStatus(@AuthenticationPrincipal SecurityUser user,@PathVariable UUID id,@Valid @RequestBody UpdateApplicationStatusRequest request){service.updateStatus(user,id,request.status(),request.comment());return ResponseEntity.noContent().build();}
    @GetMapping("/{id}/history") List<StatusHistoryResponse> history(@AuthenticationPrincipal SecurityUser user,@PathVariable UUID id){return service.history(user,id);}
}
