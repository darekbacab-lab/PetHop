package com.pethop.pet.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pethop.common.exception.ApiException;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class LocalFileStorageTest {
    @TempDir Path directory;

    @Test
    void storedImageSurvivesCreatingANewStorageInstance() throws Exception {
        LocalFileStorage first = new LocalFileStorage(directory.toString(), 10 * 1024 * 1024);
        byte[] png = imageBytes("png");
        StoredFile stored = first.store(new MockMultipartFile("file", "mascota.png", "image/png", png));

        LocalFileStorage afterRestart = new LocalFileStorage(directory.toString(), 10 * 1024 * 1024);
        assertThat(afterRestart.open(stored.path()).readAllBytes()).containsExactly(png);
        assertThat(Files.exists(directory.resolve(stored.path()))).isTrue();
    }

    @Test
    void rejectsTruncatedImageWithAValidHeader() {
        LocalFileStorage storage = new LocalFileStorage(directory.toString(), 10 * 1024 * 1024);
        byte[] truncatedPng = new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10};

        assertThatThrownBy(() -> storage.store(new MockMultipartFile("file", "mascota.png", "image/png", truncatedPng)))
                .isInstanceOf(ApiException.class)
                .hasMessage("El archivo no es una imagen JPEG, PNG o WebP válida.");
    }

    @Test
    void rejectsPathTraversalWhenOpeningAStoredPath() {
        LocalFileStorage storage = new LocalFileStorage(directory.toString(), 10 * 1024 * 1024);

        assertThatThrownBy(() -> storage.open("../secreto.png"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Archivo no encontrado.");
    }

    private byte[] imageBytes(String format) throws Exception {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Path file = Files.createTempFile(directory, "valid-image", "." + format);
        ImageIO.write(image, format, file.toFile());
        return Files.readAllBytes(file);
    }
}
