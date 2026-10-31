package com.pethop.favorite;

import com.pethop.pet.dto.PetResponse;
import com.pethop.security.SecurityUser;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/api/v1/favorites")
public class FavoriteController {
    private final FavoriteService service;public FavoriteController(FavoriteService service){this.service=service;}
    @GetMapping List<PetResponse> list(@AuthenticationPrincipal SecurityUser user){return service.list(user);}
    @PostMapping("/{petId}") @ResponseStatus(HttpStatus.CREATED) PetResponse add(@AuthenticationPrincipal SecurityUser user,@PathVariable UUID petId){return service.add(user,petId);}
    @DeleteMapping("/{petId}") ResponseEntity<Void> remove(@AuthenticationPrincipal SecurityUser user,@PathVariable UUID petId){service.remove(user,petId);return ResponseEntity.noContent().build();}
}
