package com.pethop.followup;

import com.pethop.followup.dto.FollowUpMediaResponse;
import com.pethop.followup.dto.FollowUpRequest;
import com.pethop.followup.dto.FollowUpResponse;
import com.pethop.security.SecurityUser;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class FollowUpController {
    private final FollowUpService service;public FollowUpController(FollowUpService service){this.service=service;}
    @PostMapping("/api/v1/adoptions/{id}/follow-ups") @ResponseStatus(HttpStatus.CREATED)
    FollowUpResponse create(@AuthenticationPrincipal SecurityUser user,@PathVariable UUID id,@Valid @RequestBody FollowUpRequest request){return service.create(user,id,request);}
    @GetMapping("/api/v1/adoptions/{id}/follow-ups")
    List<FollowUpResponse> list(@AuthenticationPrincipal SecurityUser user,@PathVariable UUID id){return service.list(user,id);}
    @PatchMapping("/api/v1/follow-ups/{id}") FollowUpResponse update(@AuthenticationPrincipal SecurityUser user,@PathVariable UUID id,@Valid @RequestBody FollowUpRequest request){return service.update(user,id,request);}
    @PostMapping(path="/api/v1/follow-ups/{id}/media",consumes="multipart/form-data") @ResponseStatus(HttpStatus.CREATED)
    FollowUpMediaResponse addMedia(@AuthenticationPrincipal SecurityUser user,@PathVariable UUID id,@RequestParam("file") MultipartFile file)throws IOException{return service.addMedia(user,id,file);}
}
