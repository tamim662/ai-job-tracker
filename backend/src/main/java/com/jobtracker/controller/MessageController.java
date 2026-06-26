package com.jobtracker.controller;

import com.jobtracker.dto.GenerateMessageRequest;
import com.jobtracker.dto.GeneratedMessageDto;
import com.jobtracker.service.MessageGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs/{jobId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageGenerationService service;

    @PostMapping
    public ResponseEntity<?> generate(@PathVariable Long jobId, @RequestBody GenerateMessageRequest request) {
        if (request.type() == null || request.type().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "type is required"));
        }
        try {
            return ResponseEntity.ok(service.generate(jobId, request.type()));
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if (msg != null && msg.startsWith("Invalid type")) {
                return ResponseEntity.badRequest().body(Map.of("error", msg));
            }
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<?> list(@PathVariable Long jobId) {
        try {
            List<GeneratedMessageDto> messages = service.list(jobId);
            return ResponseEntity.ok(messages);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
