package com.ia.api.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@Service
public class LocalProfilePhotoStorageService implements ProfilePhotoStorageService {
    private static final Map<String, String> EXTENSIONS_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024L * 1024L;

    private final Path storageRoot;

    public LocalProfilePhotoStorageService(@Value("${app.storage.root:./data/private-assets}") String storageRoot) {
        this.storageRoot = Path.of(storageRoot);
    }

    @Override
    public StoredPhoto store(UUID userId, MultipartFile file) throws IOException {
        validate(file);

        Files.createDirectories(storageRoot);

        String extension = EXTENSIONS_BY_CONTENT_TYPE.get(file.getContentType());
        String fileName = userId + "/" + UUID.randomUUID() + extension;
        Path target = storageRoot.resolve(fileName);
        Files.createDirectories(target.getParent());
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        return new StoredPhoto(fileName.replace('\\', '/'));
    }

    @Override
    public StoredPhotoResource load(String storageKey) throws IOException {
        Path target = storageRoot.resolve(storageKey);
        if (!Files.exists(target) || !target.normalize().startsWith(storageRoot.normalize())) {
            throw new IllegalArgumentException("Photo de profil introuvable");
        }
        String contentType = Files.probeContentType(target);
        if (contentType == null) {
            contentType = guessContentType(storageKey);
        }
        return new StoredPhotoResource(target, contentType);
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Path target = storageRoot.resolve(storageKey);
        if (target.normalize().startsWith(storageRoot.normalize())) {
            Files.deleteIfExists(target);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("La photo de profil est vide");
        }
        if (!StringUtils.hasText(file.getOriginalFilename())) {
            throw new IllegalArgumentException("Le nom de fichier de la photo est manquant");
        }
        if (!EXTENSIONS_BY_CONTENT_TYPE.containsKey(file.getContentType())) {
            throw new IllegalArgumentException("Le format de la photo doit être JPEG, PNG ou WebP");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("La photo de profil dépasse 5 Mo");
        }
    }

    private String guessContentType(String storageKey) {
        if (storageKey.endsWith(".png")) {
            return "image/png";
        }
        if (storageKey.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }
}
