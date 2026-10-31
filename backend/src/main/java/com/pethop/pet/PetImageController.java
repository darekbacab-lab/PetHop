package com.pethop.pet;

import com.pethop.pet.storage.FileStorage;
import com.pethop.security.SecurityUser;
import java.io.IOException;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pet-images")
public class PetImageController {
    private final PetService pets;
    private final FileStorage files;
    public PetImageController(PetService pets, FileStorage files) { this.pets = pets; this.files = files; }
    @GetMapping("/{id}")
    ResponseEntity<InputStreamResource> get(@PathVariable UUID id, @AuthenticationPrincipal SecurityUser principal) throws IOException {
        PetImage image = pets.image(id, principal);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(image.getContentType()))
                .contentLength(image.getFileSize()).cacheControl(CacheControl.noCache())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + image.getId() + "\"")
                .body(new InputStreamResource(files.open(image.getStoragePath())));
    }
}
