package com.iitj.mtech.sde.dataservice.controller;

import com.iitj.mtech.sde.dataservice.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Controller to handle file uploads
 */
@RestController
@RequestMapping("/upload")
public class DataController {

    private static final Logger logger = LoggerFactory.getLogger(DataController.class);

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        logger.info("Received file upload request for: {}", file.getOriginalFilename());

        try {
            // 1. Validate and store the file
            Path storedFilePath = fileStorageService.storeFile(file);
            logger.info("File stored temporarily at: {}", storedFilePath);

            // 2. Asynchronously call Analytics Service
            String analysisResult = fileStorageService.forwardToAnalytics(storedFilePath);
            logger.info("Received analysis result from Analytics Service.");

            // 3. Immediately delete the file after getting the result
            fileStorageService.deleteFile(storedFilePath);

            // 4. Return the result from Analytics Service to the UI
            return ResponseEntity.ok(analysisResult);

        } catch (IOException ex) {
            logger.error("File storage error", ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store file.", ex);
        } catch (Exception ex) {
            logger.error("Error during file processing or analytics call", ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error processing file", ex);
        }
    }
}
