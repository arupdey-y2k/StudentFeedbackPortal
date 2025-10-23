package com.iitj.mtech.sde.analyticsservice.controller;

import com.iitj.mtech.sde.analyticsservice.service.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Controller to handle analytics requests
 */
@RestController
public class AnalyticsController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsController.class);

    @Autowired
    private AnalyticsService analyticsService;

    @PostMapping("/analyze")
    public ResponseEntity<String> analyzeFeedback(@RequestParam("file") MultipartFile file) {
        logger.info("Received file for analysis: {}", file.getOriginalFilename());
        try {
            String csvData = new String(file.getBytes(), StandardCharsets.UTF_8);
            String result = analyticsService.getAnalyticsFromLlm(csvData);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            logger.error("Error reading file bytes", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error reading file", e);
        } catch (Exception e) {
            logger.error("Error during LLM analysis", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error during analysis", e);
        }
    }
}
