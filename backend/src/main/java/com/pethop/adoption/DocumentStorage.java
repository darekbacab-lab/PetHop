package com.pethop.adoption;
import com.pethop.pet.storage.StoredFile;
import java.io.IOException; import java.io.InputStream; import org.springframework.web.multipart.MultipartFile;
public interface DocumentStorage { StoredFile store(MultipartFile file) throws IOException; InputStream open(String path) throws IOException; void delete(String path) throws IOException; }
