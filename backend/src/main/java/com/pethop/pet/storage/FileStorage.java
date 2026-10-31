package com.pethop.pet.storage;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {
    StoredFile store(MultipartFile file) throws IOException;
    InputStream open(String path) throws IOException;
    void delete(String path) throws IOException;
}
