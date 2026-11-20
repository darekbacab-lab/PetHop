package com.pethop.adoption;
import com.pethop.adoption.dto.ApplicationDocumentResponse; import com.pethop.security.SecurityUser; import java.io.IOException; import java.util.UUID; import org.springframework.http.*; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.web.bind.annotation.*; import org.springframework.web.multipart.MultipartFile;
@RestController public class ApplicationDocumentController{private final ApplicationDocumentService service;public ApplicationDocumentController(ApplicationDocumentService s){service=s;}
 @PostMapping(path="/api/v1/adoption-applications/{id}/documents",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED) ApplicationDocumentResponse add(@AuthenticationPrincipal SecurityUser user,@PathVariable UUID id,@RequestParam String documentType,@RequestParam MultipartFile file)throws IOException{return service.add(user,id,documentType,file);}
}
