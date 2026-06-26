package com.jobtracker.controller;

import com.jobtracker.dto.AtsMatchRequest;
import com.jobtracker.dto.JobMatchDto;
import com.jobtracker.service.AtsMatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/jobs/{jobId}/match")
@RequiredArgsConstructor
public class AtsMatchController {

    private final AtsMatchService service;

    @PostMapping
    public ResponseEntity<?> runMatch(@PathVariable Long jobId, @RequestBody AtsMatchRequest request) {
        if (request.resumeId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "resumeId is required"));
        }
        try {
            return ResponseEntity.ok(service.match(jobId, request.resumeId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<?> getLatest(@PathVariable Long jobId) {
        try {
            return ResponseEntity.ok(service.getLatest(jobId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
