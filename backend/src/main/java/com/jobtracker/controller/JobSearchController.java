package com.jobtracker.controller;

import com.jobtracker.dto.JobSearchPageDto;
import com.jobtracker.dto.WebJobResultDto;
import com.jobtracker.repository.ResumeRepository;
import com.jobtracker.service.AdzunaService;
import com.jobtracker.service.TavilyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/job-search")
@RequiredArgsConstructor
public class JobSearchController {

    private final AdzunaService adzunaService;
    private final TavilyService tavilyService;
    private final ResumeRepository resumeRepository;

    @GetMapping
    public ResponseEntity<?> search(
            @RequestParam(required = false, defaultValue = "") String what,
            @RequestParam(required = false, defaultValue = "") String where,
            @RequestParam(required = false, defaultValue = "10") int results,
            @RequestParam(required = false, defaultValue = "1") int page) {
        if (what.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Search query (what) is required"));
        }
        String resumeText = resumeText();
        try {
            JobSearchPageDto found = adzunaService.search(what, where, Math.min(results, 20), Math.max(page, 1), resumeText);
            return ResponseEntity.ok(found);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Job search failed. Check your Adzuna API credentials."));
        }
    }

    @GetMapping("/web")
    public ResponseEntity<?> webSearch(
            @RequestParam(required = false, defaultValue = "") String what,
            @RequestParam(required = false, defaultValue = "") String where,
            @RequestParam(required = false, defaultValue = "10") int results) {
        if (what.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Search query (what) is required"));
        }
        try {
            List<WebJobResultDto> found = tavilyService.searchJobs(what, where, Math.min(results, 20), resumeText());
            return ResponseEntity.ok(found);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Web search failed. Check your Tavily API credentials."));
        }
    }

    private String resumeText() {
        return resumeRepository.findTopByOrderByCreatedAtDesc()
                .map(r -> r.getParsedText() != null ? r.getParsedText() : "")
                .orElse("");
    }
}
