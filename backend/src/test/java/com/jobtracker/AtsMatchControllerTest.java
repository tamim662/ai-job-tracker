package com.jobtracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.entity.Job;
import com.jobtracker.entity.Resume;
import com.jobtracker.repository.ApplicationRepository;
import com.jobtracker.repository.JobRepository;
import com.jobtracker.repository.ResumeRepository;
import com.jobtracker.service.GroqService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AtsMatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @MockBean
    private GroqService groqService;

    private String token;
    private Long jobId;
    private Long resumeId;

    private static final String FAKE_CLAUDE_RESPONSE = """
            {
              "ats_score": 82,
              "matched_skills": "Java, Spring Boot, REST API",
              "missing_skills": "Kubernetes, AWS Lambda",
              "suggested_summary": "Experienced Java developer with strong Spring Boot skills.",
              "suggested_skills": "Java, Spring Boot, REST API, Docker, Kubernetes",
              "suggested_experience": "Highlight microservices architecture and cloud deployments."
            }""";

    @BeforeEach
    void setUp() throws Exception {
        jobRepository.deleteAll();
        resumeRepository.deleteAll();

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "admin", "password", "changeme"))))
                .andReturn();
        token = objectMapper.readTree(login.getResponse().getContentAsString()).get("token").asText();

        MvcResult createJob = mockMvc.perform(post("/api/jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Java Developer",
                                "company", "TechCorp",
                                "description", "We need a Java/Spring Boot engineer."))))
                .andReturn();
        jobId = objectMapper.readTree(createJob.getResponse().getContentAsString()).get("id").asLong();

        Resume resume = new Resume();
        resume.setFileName("resume.pdf");
        resume.setFileUrl("https://s3.example.com/resume.pdf");
        resume.setParsedText("Java Spring Boot REST API Docker developer 5 years experience.");
        resume = resumeRepository.save(resume);
        resumeId = resume.getId();

        when(groqService.call(anyString(), anyString())).thenReturn(FAKE_CLAUDE_RESPONSE);
    }

    @Test
    void matchWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/jobs/" + jobId + "/match")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("resumeId", resumeId))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void matchMissingResumeIdReturns400() throws Exception {
        mockMvc.perform(post("/api/jobs/" + jobId + "/match")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void matchJobNotFoundReturns404() throws Exception {
        mockMvc.perform(post("/api/jobs/99999/match")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("resumeId", resumeId))))
                .andExpect(status().isNotFound());
    }

    @Test
    void matchResumeNotFoundReturns404() throws Exception {
        mockMvc.perform(post("/api/jobs/" + jobId + "/match")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("resumeId", 99999L))))
                .andExpect(status().isNotFound());
    }

    @Test
    void matchSuccessReturnsJobMatchDto() throws Exception {
        mockMvc.perform(post("/api/jobs/" + jobId + "/match")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("resumeId", resumeId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.atsScore").value(82))
                .andExpect(jsonPath("$.matchedSkills").value("Java, Spring Boot, REST API"))
                .andExpect(jsonPath("$.missingSkills").value("Kubernetes, AWS Lambda"))
                .andExpect(jsonPath("$.suggestedSummary").exists())
                .andExpect(jsonPath("$.jobId").value(jobId))
                .andExpect(jsonPath("$.resumeId").value(resumeId));
    }

    @Test
    void matchUpdatesApplicationStatusToResumeMatched() throws Exception {
        mockMvc.perform(post("/api/jobs/" + jobId + "/match")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("resumeId", resumeId))))
                .andExpect(status().isOk());

        String status = applicationRepository.findByJobId(jobId)
                .map(a -> a.getStatus())
                .orElse(null);
        assertThat(status).isEqualTo("RESUME_MATCHED");
    }

    @Test
    void getMatchWhenNoneExistsReturns404() throws Exception {
        mockMvc.perform(get("/api/jobs/" + jobId + "/match")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMatchReturnsLatestResult() throws Exception {
        mockMvc.perform(post("/api/jobs/" + jobId + "/match")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("resumeId", resumeId))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/jobs/" + jobId + "/match")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.atsScore").value(82))
                .andExpect(jsonPath("$.matchedSkills").value("Java, Spring Boot, REST API"));
    }
}
