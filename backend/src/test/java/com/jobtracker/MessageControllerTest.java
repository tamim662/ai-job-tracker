package com.jobtracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.repository.JobRepository;
import com.jobtracker.service.ClaudeService;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JobRepository jobRepository;

    @MockBean
    private ClaudeService claudeService;

    @MockBean
    private GroqService groqService;

    private String token;
    private Long jobId;

    @BeforeEach
    void setUp() throws Exception {
        jobRepository.deleteAll();

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
                                "title", "Backend Engineer",
                                "company", "Acme Corp",
                                "description", "We need a Spring Boot engineer."))))
                .andReturn();
        jobId = objectMapper.readTree(createJob.getResponse().getContentAsString()).get("id").asLong();

        when(claudeService.call(anyString(), anyString())).thenReturn(
                "Subject: Application for Backend Engineer at Acme Corp\n\nDear Hiring Manager...");
    }

    @Test
    void generateWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/jobs/" + jobId + "/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("type", "HR_EMAIL"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void generateMissingTypeReturns400() throws Exception {
        mockMvc.perform(post("/api/jobs/" + jobId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void generateInvalidTypeReturns400() throws Exception {
        mockMvc.perform(post("/api/jobs/" + jobId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("type", "INVALID_TYPE"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generateJobNotFoundReturns404() throws Exception {
        mockMvc.perform(post("/api/jobs/99999/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("type", "HR_EMAIL"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void generateHrEmailReturnsMessage() throws Exception {
        mockMvc.perform(post("/api/jobs/" + jobId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("type", "HR_EMAIL"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("HR_EMAIL"))
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.jobId").value(jobId));
    }

    @Test
    void generateLinkedInMessage() throws Exception {
        when(claudeService.call(anyString(), anyString())).thenReturn(
                "Hi Sarah, I came across the Backend Engineer role at Acme Corp — your work on distributed systems aligns with my 5 years of Spring Boot experience.");

        mockMvc.perform(post("/api/jobs/" + jobId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("type", "LINKEDIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("LINKEDIN"));
    }

    @Test
    void listMessagesReturnsAllGenerated() throws Exception {
        mockMvc.perform(post("/api/jobs/" + jobId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("type", "HR_EMAIL"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/jobs/" + jobId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("type", "FOLLOWUP"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/jobs/" + jobId + "/messages")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void listMessagesJobNotFoundReturns404() throws Exception {
        mockMvc.perform(get("/api/jobs/99999/messages")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void generateCoverLetterReturnsMessage() throws Exception {
        when(claudeService.call(anyString(), anyString())).thenReturn(
                "Dear Hiring Manager,\n\nI am writing to apply for the Backend Engineer position at Acme Corp...");

        mockMvc.perform(post("/api/jobs/" + jobId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("type", "COVER_LETTER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("COVER_LETTER"))
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.jobId").value(jobId));
    }
}
