package com.ia.api.user.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

public interface ProfilePhotoStorageService {
    StoredPhoto store(UUID userId, MultipartFile file) throws IOException;
    StoredPhotoResource load(String storageKey) throws IOException;
    void delete(String storageKey) throws IOException;

    record StoredPhoto(String storageKey) {
    }

    record StoredPhotoResource(Path path, String contentType) {
    }
}
