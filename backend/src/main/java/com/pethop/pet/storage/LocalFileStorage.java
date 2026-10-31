package com.pethop.pet.storage;

import com.pethop.common.exception.ApiException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class LocalFileStorage implements FileStorage {
    private static final Pattern SAFE_PATH = Pattern.compile("^[0-9a-fA-F-]{36}\\.(jpg|png|webp)$");
    private final Path root;
    private final long maximumSize;
    public LocalFileStorage(@Value("${pethop.storage.local-path}") String root,
            @Value("${pethop.storage.max-image-size}") long maximumSize) {
        this.root = Paths.get(root).toAbsolutePath().normalize();
        this.maximumSize = maximumSize;
    }
    @Override public StoredFile store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw ApiException.badRequest("Selecciona una imagen.");
        if (file.getSize() > maximumSize) throw ApiException.badRequest("La imagen excede el tamaño máximo de 10 MB.");
        byte[] header;
        try (InputStream input = file.getInputStream()) { header = input.readNBytes(16); }
        String type = detectType(header);
        if (type == null) throw ApiException.badRequest("El archivo no es una imagen JPEG, PNG o WebP válida.");
        String suppliedType = file.getContentType();
        if (suppliedType != null && !suppliedType.isBlank() && !suppliedType.equalsIgnoreCase(type)) {
            throw ApiException.badRequest("El tipo declarado no coincide con el contenido real de la imagen.");
        }
        String original = safeName(file.getOriginalFilename());
        String extension = extension(original);
        if (!extension.equals(extensionFor(type))) throw ApiException.badRequest("La extensión del archivo no coincide con el formato real.");
        String relative = UUID.randomUUID() + "." + extension;
        Files.createDirectories(root);
        Path destination = root.resolve(relative).normalize();
        if (!destination.getParent().equals(root)) throw ApiException.badRequest("Nombre de archivo no válido.");
        try (InputStream input = file.getInputStream()) { Files.copy(input, destination); }
        return new StoredFile(relative, type, file.getSize(), original);
    }
    @Override public InputStream open(String path) throws IOException { return Files.newInputStream(resolve(path)); }
    @Override public void delete(String path) throws IOException { Files.deleteIfExists(resolve(path)); }
    private Path resolve(String path) {
        if (path == null || !SAFE_PATH.matcher(path).matches()) throw ApiException.notFound("Archivo no encontrado.");
        Path file = root.resolve(path).normalize();
        if (!file.getParent().equals(root)) throw ApiException.notFound("Archivo no encontrado.");
        return file;
    }
    private String detectType(byte[] data) {
        if (data.length >= 3 && (data[0]&255)==0xff && (data[1]&255)==0xd8 && (data[2]&255)==0xff) return "image/jpeg";
        if (data.length >= 8 && (data[0]&255)==137 && data[1]==80 && data[2]==78 && data[3]==71 && data[4]==13 && data[5]==10 && data[6]==26 && data[7]==10) return "image/png";
        if (data.length >= 12 && data[0]==82 && data[1]==73 && data[2]==70 && data[3]==70 && data[8]==87 && data[9]==69 && data[10]==66 && data[11]==80) return "image/webp";
        return null;
    }
    private String safeName(String name) {
        if (name == null || name.isBlank()) throw ApiException.badRequest("El archivo debe tener nombre.");
        String base = Paths.get(name.replace('\\', '/')).getFileName().toString();
        String safe = base.replaceAll("[^\\p{L}\\p{N}._ -]", "_");
        return safe.length() > 255 ? safe.substring(safe.length() - 255) : safe;
    }
    private String extension(String name) { int dot = name.lastIndexOf('.'); return dot < 0 ? "" : name.substring(dot+1).toLowerCase(Locale.ROOT); }
    private String extensionFor(String type) { return switch(type) { case "image/jpeg" -> "jpg"; case "image/png" -> "png"; case "image/webp" -> "webp"; default -> ""; }; }
}
