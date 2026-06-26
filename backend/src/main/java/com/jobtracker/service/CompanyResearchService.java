package com.jobtracker.service;

import com.jobtracker.dto.CompanyResearchDto;
import com.jobtracker.entity.CompanyResearch;
import com.jobtracker.entity.Job;
import com.jobtracker.repository.CompanyResearchRepository;
import com.jobtracker.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyResearchService {

    private static final String SYSTEM_PROMPT = """
            You are a career intelligence analyst helping a job candidate prepare for an interview. \
            Using the web search results provided, write a structured company briefing in markdown. \
            Be specific and factual. Use bullet points. Keep each section concise.""";

    private final TavilyService tavilyService;
    private final ClaudeService claudeService;
    private final JobRepository jobRepository;
    private final CompanyResearchRepository researchRepository;

    @Transactional
    public CompanyResearchDto research(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        String company = job.getCompany() != null && !job.getCompany().isBlank()
                ? job.getCompany() : job.getTitle();

        List<String> results1 = tavilyService.search(company + " company products services technology stack culture", 5);
        List<String> results2 = tavilyService.search(company + " company interview process recent news 2025", 5);

        StringBuilder context = new StringBuilder();
        int i = 1;
        for (String r : results1) {
            context.append("[").append(i++).append("]\n").append(r).append("\n\n");
        }
        for (String r : results2) {
            context.append("[").append(i++).append("]\n").append(r).append("\n\n");
        }

        String userMessage = """
                Company: %s
                Role applied for: %s
                Location: %s

                Web search results:
                %s

                Write a briefing with exactly these sections:
                ## Company Overview
                ## Products & Services
                ## Tech Stack
                ## Engineering Culture & Work Style
                ## Recent News & Developments
                ## Interview Preparation Tips
                """.formatted(company, job.getTitle(),
                job.getLocation() != null ? job.getLocation() : "",
                context.toString());

        String briefing = claudeService.call(SYSTEM_PROMPT, userMessage);

        researchRepository.findTopByJobIdOrderByCreatedAtDesc(jobId)
                .ifPresent(researchRepository::delete);

        CompanyResearch research = new CompanyResearch();
        research.setJobId(jobId);
        research.setCompanyName(company);
        research.setBriefing(briefing);
        research = researchRepository.save(research);

        return toDto(research);
    }

    @Transactional(readOnly = true)
    public CompanyResearchDto getLatest(Long jobId) {
        return researchRepository.findTopByJobIdOrderByCreatedAtDesc(jobId)
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("No research found for job: " + jobId));
    }

    private CompanyResearchDto toDto(CompanyResearch r) {
        return new CompanyResearchDto(r.getId(), r.getJobId(), r.getCompanyName(), r.getBriefing(), r.getCreatedAt());
    }
}
