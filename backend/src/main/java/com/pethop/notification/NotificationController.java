package com.pethop.notification;

import com.pethop.common.response.PageResponse;
import com.pethop.notification.dto.NotificationResponse;
import com.pethop.security.SecurityUser;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated @RestController @RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service){this.service=service;}
    @GetMapping PageResponse<NotificationResponse> list(@AuthenticationPrincipal SecurityUser user,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="20") @Min(1) @Max(50) int size){return service.list(user,page,size);}
    @PatchMapping("/{id}/read") ResponseEntity<Void> read(@AuthenticationPrincipal SecurityUser user,@PathVariable UUID id){service.markRead(user,id);return ResponseEntity.noContent().build();}
}
