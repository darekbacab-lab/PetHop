package com.pethop.affiliate;

import com.pethop.affiliate.dto.AffiliateRequest;
import com.pethop.affiliate.dto.AffiliateResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class AffiliateController {
    private final AffiliateService service;
    public AffiliateController(AffiliateService service){this.service=service;}
    @GetMapping("/api/v1/affiliates") List<AffiliateResponse> list(){return service.listPublic();}
    @GetMapping("/api/v1/admin/affiliates") List<AffiliateResponse> listAdmin(){return service.listAdmin();}
    @PostMapping("/api/v1/admin/affiliates") @ResponseStatus(HttpStatus.CREATED) AffiliateResponse create(@Valid @RequestBody AffiliateRequest request){return service.create(request);}
    @PatchMapping("/api/v1/admin/affiliates/{id}") AffiliateResponse update(@PathVariable UUID id,@Valid @RequestBody AffiliateRequest request){return service.update(id,request);}
    @DeleteMapping("/api/v1/admin/affiliates/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void delete(@PathVariable UUID id){service.delete(id);}
}
