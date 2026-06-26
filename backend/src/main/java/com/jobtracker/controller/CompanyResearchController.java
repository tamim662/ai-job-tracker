package com.jobtracker.controller;

import com.jobtracker.service.CompanyResearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/jobs/{jobId}/research")
@RequiredArgsConstructor
public class CompanyResearchController {

    private final CompanyResearchService service;

    @PostMapping
    public ResponseEntity<?> generate(@PathVariable Long jobId) {
        try {
            return ResponseEntity.ok(service.research(jobId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Company research failed: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getLatest(@PathVariable Long jobId) {
        try {
            return ResponseEntity.ok(service.getLatest(jobId));
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if (msg != null && msg.startsWith("No research")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.notFound().build();
        }
    }
}
