package com.marketingfacil.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {
    private final Path uploadDirectory;

    public FileStorageService(@Value("${app.upload-dir:/app/uploads}") String uploadDirectory) throws IOException {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
        Files.createDirectories(this.uploadDirectory);
    }

    public String storeOriginal(MultipartFile file, String extension) throws IOException {
        String filename = UUID.randomUUID() + extension;
        file.transferTo(resolve(filename));
        return filename;
    }

    public String newOutputName(String suffix) {
        return UUID.randomUUID() + "-" + suffix;
    }

    public Path resolve(String generatedName) throws IOException {
        if (generatedName == null || !generatedName.matches(
                "[a-f0-9-]{36}(?:\\.(?:jpg|png)|-(?:cuadrada|vertical|horizontal)\\.png|-video\\.mp4)")) {
            throw new IOException("Nombre de archivo inválido.");
        }
        Path resolved = uploadDirectory.resolve(generatedName).normalize();
        if (!resolved.getParent().equals(uploadDirectory)) {
            throw new IOException("Ruta de archivo inválida.");
        }
        return resolved;
    }
}
