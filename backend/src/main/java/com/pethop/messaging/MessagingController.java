package com.pethop.messaging;

import com.pethop.common.response.PageResponse;
import com.pethop.messaging.dto.ConversationResponse;
import com.pethop.messaging.dto.CreateConversationRequest;
import com.pethop.messaging.dto.MessageResponse;
import com.pethop.messaging.dto.SendMessageRequest;
import com.pethop.security.SecurityUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
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

@Validated @RestController @RequestMapping("/api/v1")
public class MessagingController {
    private final MessagingService service;public MessagingController(MessagingService service){this.service=service;}
    @GetMapping("/conversations") List<ConversationResponse> conversations(@AuthenticationPrincipal SecurityUser user){return service.list(user);}
    @PostMapping("/conversations") @ResponseStatus(HttpStatus.CREATED)
    ConversationResponse create(@AuthenticationPrincipal SecurityUser user,@Valid @RequestBody CreateConversationRequest request){return service.create(user,request.petId(),request.applicationId());}
    @GetMapping("/conversations/{id}/messages") PageResponse<MessageResponse> messages(@AuthenticationPrincipal SecurityUser user,@PathVariable UUID id,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="50") @Min(1) @Max(100) int size){return service.messages(user,id,page,size);}
    @PostMapping("/conversations/{id}/messages") @ResponseStatus(HttpStatus.CREATED)
    MessageResponse send(@AuthenticationPrincipal SecurityUser user,@PathVariable UUID id,@Valid @RequestBody SendMessageRequest request){return service.send(user,id,request.content());}
}
