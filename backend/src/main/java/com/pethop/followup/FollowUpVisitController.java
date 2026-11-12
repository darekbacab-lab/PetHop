package com.pethop.followup;
import com.pethop.followup.dto.FollowUpVisitRequest;
import com.pethop.followup.dto.FollowUpVisitResponse;
import com.pethop.security.SecurityUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class FollowUpVisitController {
    private final FollowUpVisitService service; public FollowUpVisitController(FollowUpVisitService service){this.service=service;}
    @PostMapping("/api/v1/adoptions/{id}/visits") @ResponseStatus(HttpStatus.CREATED) FollowUpVisitResponse create(@AuthenticationPrincipal SecurityUser user,@PathVariable UUID id,@Valid @RequestBody FollowUpVisitRequest request){return service.create(user,id,request);}
    @GetMapping("/api/v1/adoptions/{id}/visits") List<FollowUpVisitResponse> list(@AuthenticationPrincipal SecurityUser user,@PathVariable UUID id){return service.list(user,id);}
    @PatchMapping("/api/v1/visits/{id}") FollowUpVisitResponse update(@AuthenticationPrincipal SecurityUser user,@PathVariable UUID id,@Valid @RequestBody FollowUpVisitRequest request){return service.update(user,id,request);}
}
