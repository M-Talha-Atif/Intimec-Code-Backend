package com.aidentx.core.onboarding.discovery.controller;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for managing connector data files (CSV, JSON, etc.)
 * Provides file upload, download, list, and delete operations
 * for connectors that require file-based configuration.
 */
@RestController
@RequestMapping("/api/v1/connector-files")
public class ConnectorFileController {

    private final IUnifiedLogger logger;
    private final String uploadDirectory;

    public ConnectorFileController(
            UnifiedLoggerFactory loggerFactory,
            @Value("${connector.file.upload-directory:/app/connector-data}") String uploadDirectory) {
        this.logger = loggerFactory.getLogger("ConnectorFileController");
        this.uploadDirectory = uploadDirectory;
        
        // Ensure upload directory exists
        try {
            Files.createDirectories(Paths.get(uploadDirectory));
            logger.info().message("Connector file upload directory initialized").detail("path", uploadDirectory).log();
        } catch (IOException e) {
            logger.error(e).message("Failed to create upload directory").detail("path", uploadDirectory).log();
            throw new RuntimeException("Failed to initialize file upload directory", e);
        }
    }

    /**
     * Upload a file for connector configuration
     * POST /api/v1/connector-files/upload
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();

        if (file.isEmpty()) {
            response.put("success", false);
            response.put("message", "No file provided");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // Sanitize filename
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                response.put("success", false);
                response.put("message", "Invalid filename");
                return ResponseEntity.badRequest().body(response);
            }

            // Remove path separators and special characters
            String sanitizedFilename = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
            
            // Save file
            Path targetPath = Paths.get(uploadDirectory).resolve(sanitizedFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            logger.info()
                .message("File uploaded successfully")
                .detail("filename", sanitizedFilename)
                .detail("size", file.getSize())
                .log();

            response.put("success", true);
            response.put("message", "File uploaded successfully");
            response.put("filename", sanitizedFilename);
            response.put("path", targetPath.toString());
            response.put("size", file.getSize());

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            logger.error(e).message("Failed to upload file").detail("filename", file.getOriginalFilename()).log();
            response.put("success", false);
            response.put("message", "Failed to upload file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * List all uploaded connector files
     * GET /api/v1/connector-files
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listFiles() {
        Map<String, Object> response = new HashMap<>();

        try {
            File directory = new File(uploadDirectory);
            File[] files = directory.listFiles();

            List<Map<String, Object>> fileList = files == null ? List.of() : Arrays.stream(files)
                .filter(File::isFile)
                .map(file -> {
                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put("filename", file.getName());
                    fileInfo.put("path", file.getAbsolutePath());
                    fileInfo.put("size", file.length());
                    fileInfo.put("lastModified", file.lastModified());
                    return fileInfo;
                })
                .collect(Collectors.toList());

            response.put("success", true);
            response.put("files", fileList);
            response.put("count", fileList.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error(e).message("Failed to list files").log();
            response.put("success", false);
            response.put("message", "Failed to list files: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Download a connector file
     * GET /api/v1/connector-files/{filename}
     */
    @GetMapping("/{filename}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(uploadDirectory).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                logger.warn().message("File not found or not readable").detail("filename", filename).log();
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);

        } catch (MalformedURLException e) {
            logger.error(e).message("Invalid file path").detail("filename", filename).log();
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            logger.error(e).message("Error reading file").detail("filename", filename).log();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete a connector file
     * DELETE /api/v1/connector-files/{filename}
     */
    @DeleteMapping("/{filename}")
    public ResponseEntity<Map<String, Object>> deleteFile(@PathVariable String filename) {
        Map<String, Object> response = new HashMap<>();

        try {
            Path filePath = Paths.get(uploadDirectory).resolve(filename).normalize();
            
            if (!Files.exists(filePath)) {
                response.put("success", false);
                response.put("message", "File not found");
                return ResponseEntity.notFound().build();
            }

            Files.delete(filePath);

            logger.info().message("File deleted successfully").detail("filename", filename).log();

            response.put("success", true);
            response.put("message", "File deleted successfully");
            response.put("filename", filename);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            logger.error(e).message("Failed to delete file").detail("filename", filename).log();
            response.put("success", false);
            response.put("message", "Failed to delete file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
