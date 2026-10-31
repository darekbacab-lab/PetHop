package com.pethop.pet;

import com.pethop.common.response.PageResponse;
import com.pethop.pet.dto.PetImageResponse;
import com.pethop.pet.dto.PetRequest;
import com.pethop.pet.dto.PetResponse;
import com.pethop.pet.dto.UpdatePetStatusRequest;
import com.pethop.security.SecurityUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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

@Validated
@RestController
@RequestMapping("/api/v1/pets")
public class PetController {
    private final PetService service;
    public PetController(PetService service) { this.service = service; }

    @GetMapping
    PageResponse<PetResponse> list(@RequestParam(required=false) PetSpecies species,
            @RequestParam(required=false) PetSex sex, @RequestParam(required=false) PetSize petSize,
            @RequestParam(required=false) String location, @RequestParam(required=false) String search,
            @RequestParam(defaultValue="recommended") String sort,
            @RequestParam(defaultValue="0") @Min(0) int page,
            @RequestParam(defaultValue="12") @Min(1) @Max(48) int pageSize,
            @AuthenticationPrincipal SecurityUser principal) {
        return service.list(species, sex, petSize, location, search, sort, page, pageSize, principal);
    }

    @GetMapping("/mine")
    PageResponse<PetResponse> mine(@RequestParam(required=false) PetSpecies species,
            @RequestParam(required=false) PetSex sex, @RequestParam(required=false) PetSize petSize,
            @RequestParam(required=false) String location, @RequestParam(required=false) String search,
            @RequestParam(defaultValue="newest") String sort,
            @RequestParam(defaultValue="0") @Min(0) int page,
            @RequestParam(defaultValue="24") @Min(1) @Max(48) int pageSize,
            @AuthenticationPrincipal SecurityUser principal) {
        return service.list(species, sex, petSize, location, search, sort, page, pageSize, principal);
    }

    @GetMapping("/{id}") PetResponse get(@PathVariable UUID id, @AuthenticationPrincipal SecurityUser principal) { return service.get(id, principal); }
    @GetMapping("/{id}/images") List<PetImageResponse> images(@PathVariable UUID id, @AuthenticationPrincipal SecurityUser principal) { return service.images(id, principal); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    PetResponse create(@AuthenticationPrincipal SecurityUser principal, @Valid @RequestBody PetRequest request) { return service.create(principal, request); }
    @PatchMapping("/{id}") PetResponse update(@PathVariable UUID id, @AuthenticationPrincipal SecurityUser principal, @Valid @RequestBody PetRequest request) { return service.update(id, principal, request); }
    @PatchMapping("/{id}/status") PetResponse updateStatus(@PathVariable UUID id, @AuthenticationPrincipal SecurityUser principal, @Valid @RequestBody UpdatePetStatusRequest request) { return service.updateStatus(id, principal, request.adoptionStatus()); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void delete(@PathVariable UUID id, @AuthenticationPrincipal SecurityUser principal) { service.delete(id, principal); }

    @PostMapping(path="/{id}/images", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    PetImageResponse addImage(@PathVariable UUID id, @AuthenticationPrincipal SecurityUser principal, @RequestParam("file") MultipartFile file) throws IOException {
        return service.addImage(id, principal, file);
    }
    @DeleteMapping("/{id}/images/{imageId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteImage(@PathVariable UUID id, @PathVariable UUID imageId, @AuthenticationPrincipal SecurityUser principal) throws IOException { service.deleteImage(id, imageId, principal); }
}
