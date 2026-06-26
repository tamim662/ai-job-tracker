package com.jobtracker.controller;

import com.jobtracker.dto.CreateJobRequest;
import com.jobtracker.dto.JobDto;
import com.jobtracker.dto.UpdateJobRequest;
import com.jobtracker.dto.UpdateStatusRequest;
import com.jobtracker.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService service;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateJobRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Job title is required"));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    public ResponseEntity<List<JobDto>> list() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.findById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody UpdateJobRequest request) {
        try {
            return ResponseEntity.ok(service.update(id, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody UpdateStatusRequest request) {
        if (request.status() == null || request.status().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "status is required"));
        }
        try {
            service.updateStatus(id, request.status());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if (msg != null && msg.startsWith("Invalid status")) {
                return ResponseEntity.badRequest().body(Map.of("error", msg));
            }
            return ResponseEntity.notFound().build();
        }
    }
}
