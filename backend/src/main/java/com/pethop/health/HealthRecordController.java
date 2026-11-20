package com.pethop.health;

import com.pethop.health.dto.HealthRecordRequest;
import com.pethop.health.dto.HealthRecordResponse;
import com.pethop.security.SecurityUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/api/v1/pets/{petId}/health-records")
public class HealthRecordController {
    private final HealthRecordService service;public HealthRecordController(HealthRecordService service){this.service=service;}
    @GetMapping List<HealthRecordResponse> list(@AuthenticationPrincipal SecurityUser user,@PathVariable UUID petId){return service.list(user,petId);}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) HealthRecordResponse create(@AuthenticationPrincipal SecurityUser user,@PathVariable UUID petId,@Valid @RequestBody HealthRecordRequest request){return service.create(user,petId,request);}
    @PutMapping("/{recordId}") HealthRecordResponse update(@AuthenticationPrincipal SecurityUser user,@PathVariable UUID petId,@PathVariable UUID recordId,@Valid @RequestBody HealthRecordRequest request){return service.update(user,petId,recordId,request);}
    @DeleteMapping("/{recordId}") @ResponseStatus(HttpStatus.NO_CONTENT) void delete(@AuthenticationPrincipal SecurityUser user,@PathVariable UUID petId,@PathVariable UUID recordId){service.delete(user,petId,recordId);}
}
