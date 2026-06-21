package com.aidentx.core.onboarding.service;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;
import org.springframework.core.io.Resource;

/**
 * Service for managing file storage, specifically for application icons.
 */
@Service
public class FileStorageService {

    private final IUnifiedLogger logger;
    private final Path iconStorageLocation;

    public FileStorageService(UnifiedLoggerFactory loggerFactory,
            @Value("${file.storage.location:./storage}") String storageLocation) {
        this.logger = loggerFactory.getLogger(FileStorageService.class);
        this.iconStorageLocation = Paths.get(storageLocation, "icons").toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.iconStorageLocation);
            logger.info().message("Initialized icon storage directory.")
                    .detail("path", this.iconStorageLocation.toString()).log();
        } catch (IOException e) {
            logger.error(e).message("Could not create icon storage directory.")
                    .detail("path", this.iconStorageLocation.toString()).log();
            throw new RuntimeException("Could not initialize file storage", e);
        }
    }

    /**
     * Stores an application icon.
     *
     * @param file  The icon file from the multipart request.
     * @param appId The UUID of the application.
     * @return The relative path where the file is stored.
     * @throws IOException              if the file cannot be stored.
     * @throws IllegalArgumentException if the file is not a valid image.
     */
    public String storeIcon(MultipartFile file, UUID appId) throws IOException {
        validateIcon(file);

        String originalFilename = Objects.requireNonNull(file.getOriginalFilename());
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFilename = appId.toString() + fileExtension;

        Path targetLocation = this.iconStorageLocation.resolve(newFilename);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        logger.info().message("Stored application icon.").detail("appId", appId)
                .detail("path", targetLocation.toString()).log();

        // Return a relative path or identifier for storage in the DB
        return Paths.get("icons", newFilename).toString();
    }

    /**
     * Deletes the icon file associated with an application.
     *
     * @param iconPath The path of the icon to delete.
     */
    public void deleteIcon(String iconPath) {
        if (iconPath == null || iconPath.isBlank()) {
            return;
        }
        try {
            Path filePath = this.iconStorageLocation.resolve(Paths.get(iconPath).getFileName()).normalize();
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                logger.info().message("Deleted application icon file.")
                        .detail("path", filePath.toString()).log();
            }
        } catch (IOException e) {
            logger.warn().message("Failed to delete icon file.")
                    .detail("path", iconPath).log();
        }
    }

    private void validateIcon(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store an empty file.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Invalid file type. Only images are allowed. Provided: " + contentType);
        }
    }

    /**
     * Loads an icon file as a Spring Resource.
     *
     * @param filename The name of the file to load (e.g., "uuid.png").
     * @return The file as a Resource.
     * @throws FileNotFoundException if the file does not exist.
     */
    public Resource loadIconAsResource(String filename) throws FileNotFoundException {
        try {
            Path filePath = this.iconStorageLocation.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new FileNotFoundException("Could not read file: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new FileNotFoundException("Could not read file: " + filename);
        }
    }
}