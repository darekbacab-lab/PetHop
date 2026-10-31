package com.pethop.followup;

import com.pethop.pet.storage.FileStorage;
import com.pethop.security.SecurityUser;
import java.io.IOException;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/api/v1/follow-up-media")
public class FollowUpMediaController {
    private final FollowUpService service;private final FileStorage files;
    public FollowUpMediaController(FollowUpService service,FileStorage files){this.service=service;this.files=files;}
    @GetMapping("/{id}") ResponseEntity<InputStreamResource> get(@AuthenticationPrincipal SecurityUser user,@PathVariable UUID id)throws IOException{
        FollowUpMedia media=service.getMedia(user,id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(media.getContentType())).contentLength(media.getFileSize()).cacheControl(CacheControl.noCache()).body(new InputStreamResource(files.open(media.getStoragePath())));
    }
}
