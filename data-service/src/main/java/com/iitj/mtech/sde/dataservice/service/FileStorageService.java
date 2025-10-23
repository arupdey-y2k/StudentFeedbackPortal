package com.iitj.mtech.sde.dataservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service to manage file storage and communication with Analytics Service
 */
@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
    private final Path fileStorageLocation;
    private final long maxFileSize = 10 * 1024 * 1024; // 10 MB
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Autowired
    private RestTemplate restTemplate;

    @Value("${analytics.service.url}")
    private String analyticsServiceUrl; // e.g., http://ANALYTICS-SERVICE/analyze

    @Autowired
    public FileStorageService(@Value("${file.upload-dir}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
            logger.info("Upload directory created at: {}", this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public Path storeFile(MultipartFile file) throws IOException {
        // 1. Validate file size
        if (file.getSize() > maxFileSize) {
            logger.warn("File size exceeds limit: {}", file.getSize());
            throw new IOException("File size exceeds 10MB limit.");
        }

        // 2. Validate file type (basic check for CSV)
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("text/csv") && !file.getOriginalFilename().endsWith(".csv"))) {
            logger.warn("Invalid file type: {}", contentType);
            throw new IOException("Invalid file format. Only CSV files are allowed.");
        }

        // 3. Generate a unique filename to avoid collisions
        String uniqueFileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);

        // 4. Save the file
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        // 5. Schedule deletion for 1 hour later (as a fallback)
        // Note: The primary deletion happens immediately after analytics.
        scheduleFileDeletion(targetLocation, 1, TimeUnit.HOURS);

        return targetLocation;
    }

    public String forwardToAnalytics(Path filePath) throws IOException {
        logger.info("Forwarding file to Analytics Service: {}", filePath.getFileName());

        // Create a FileSystemResource from the saved file
        Resource resource = new FileSystemResource(filePath.toFile());

        // Create the request body as Multipart
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // Create the HTTP entity
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // Call the Analytics Service using its Eureka name
        // The URL "http://ANALYTICS-SERVICE/analyze" will be resolved by Eureka
        ResponseEntity<String> response = restTemplate.postForEntity(
                analyticsServiceUrl,
                requestEntity,
                String.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        } else {
            logger.error("Analytics Service returned error: {}", response.getStatusCode());
            throw new RuntimeException("Failed to get analysis from Analytics Service. Status: " + response.getStatusCode());
        }
    }

    public void deleteFile(Path filePath) {
        try {
            Files.deleteIfExists(filePath);
            logger.info("Deleted temporary file: {}", filePath.getFileName());
        } catch (IOException e) {
            logger.error("Could not delete file: {}", filePath.getFileName(), e);
        }
    }

    private void scheduleFileDeletion(Path filePath, long delay, TimeUnit unit) {
        scheduler.schedule(() -> {
            deleteFile(filePath);
        }, delay, unit);
        logger.info("Scheduled deletion for {} in {} {}", filePath.getFileName(), delay, unit.toString());
    }
}
