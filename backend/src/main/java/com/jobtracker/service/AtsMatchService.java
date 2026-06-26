package com.jobtracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.dto.JobMatchDto;
import com.jobtracker.entity.Application;
import com.jobtracker.entity.Job;
import com.jobtracker.entity.JobMatch;
import com.jobtracker.entity.Resume;
import com.jobtracker.repository.ApplicationRepository;
import com.jobtracker.repository.JobMatchRepository;
import com.jobtracker.repository.JobRepository;
import com.jobtracker.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AtsMatchService {

    private static final String SYSTEM_PROMPT = """
            You are an expert ATS (Applicant Tracking System) analyst and career coach \
            specialising in the Australian job market. Analyse the provided job description \
            and resume, then return ONLY a valid JSON object with exactly these fields:
            - "ats_score": integer from 0 to 100 representing overall ATS compatibility
            - "matched_skills": comma-separated list of skills/keywords found in both resume and job description
            - "missing_skills": comma-separated list of key skills/keywords from the job description not found in the resume
            - "suggested_summary": an improved professional summary tailored to this specific job
            - "suggested_skills": improved skills section content with relevant keywords added
            - "suggested_experience": suggestions for reframing experience bullet points to better match job requirements
            Return ONLY the JSON object — no markdown, no explanation, no code blocks.""";

    private final GroqService groqService;
    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;
    private final JobMatchRepository jobMatchRepository;
    private final ApplicationRepository applicationRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public JobMatchDto match(Long jobId, Long resumeId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found: " + resumeId));

        String userMessage = "JOB DESCRIPTION:\n" + nullSafe(job.getDescription())
                + "\n\nRESUME:\n" + nullSafe(resume.getParsedText());

        String responseText = groqService.call(SYSTEM_PROMPT, userMessage);

        JsonNode json;
        try {
            json = objectMapper.readTree(responseText);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Claude response as JSON: " + responseText, e);
        }

        JobMatch match = new JobMatch();
        match.setJobId(jobId);
        match.setResumeId(resumeId);
        match.setAtsScore(json.path("ats_score").asInt());
        match.setMatchedSkills(json.path("matched_skills").asText());
        match.setMissingSkills(json.path("missing_skills").asText());
        match.setSuggestedSummary(json.path("suggested_summary").asText());
        match.setSuggestedSkills(json.path("suggested_skills").asText());
        match.setSuggestedExperience(json.path("suggested_experience").asText());
        match = jobMatchRepository.save(match);

        applicationRepository.findByJobId(jobId).ifPresent(app -> {
            app.setStatus("RESUME_MATCHED");
            applicationRepository.save(app);
        });

        return toDto(match);
    }

    @Transactional(readOnly = true)
    public JobMatchDto getLatest(Long jobId) {
        return jobMatchRepository.findTopByJobIdOrderByCreatedAtDesc(jobId)
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("No match found for job: " + jobId));
    }

    private JobMatchDto toDto(JobMatch m) {
        return new JobMatchDto(
                m.getId(), m.getJobId(), m.getResumeId(), m.getAtsScore(),
                m.getMatchedSkills(), m.getMissingSkills(),
                m.getSuggestedSummary(), m.getSuggestedSkills(), m.getSuggestedExperience(),
                m.getCreatedAt());
    }

    private String nullSafe(String s) {
        return s != null ? s : "";
    }
}
