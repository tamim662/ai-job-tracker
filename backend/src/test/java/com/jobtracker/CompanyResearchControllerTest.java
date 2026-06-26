package com.jobtracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.service.ClaudeService;
import com.jobtracker.service.TavilyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CompanyResearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TavilyService tavilyService;

    @MockBean
    private ClaudeService claudeService;

    private String token;
    private Long jobId;

    @BeforeEach
    void setUp() throws Exception {
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
                                "title", "Software Engineer",
                                "company", "Atlassian",
                                "location", "Sydney, NSW"))))
                .andReturn();
        jobId = objectMapper.readTree(createJob.getResponse().getContentAsString()).get("id").asLong();

        when(tavilyService.search(anyString(), anyInt())).thenReturn(List.of(
                "[Atlassian]\nAtlassian is a leading enterprise software company known for Jira, Confluence, and Trello."
        ));
        when(claudeService.call(anyString(), anyString())).thenReturn(
                "## Company Overview\nAtlassian is a global enterprise software company.\n\n## Products & Services\n- Jira\n- Confluence\n- Trello"
        );
    }

    @Test
    void researchWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/jobs/" + jobId + "/research"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void researchNonExistentJobReturns404() throws Exception {
        mockMvc.perform(post("/api/jobs/99999/research")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void researchGeneratesAndReturnsBriefing() throws Exception {
        mockMvc.perform(post("/api/jobs/" + jobId + "/research")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(jobId))
                .andExpect(jsonPath("$.companyName").value("Atlassian"))
                .andExpect(jsonPath("$.briefing").exists())
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void getLatestResearchReturns404WhenNoneExists() throws Exception {
        MvcResult createJob2 = mockMvc.perform(post("/api/jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "New Job No Research"))))
                .andReturn();
        Long newJobId = objectMapper.readTree(createJob2.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/jobs/" + newJobId + "/research")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void getLatestResearchReturnsExistingBriefing() throws Exception {
        mockMvc.perform(post("/api/jobs/" + jobId + "/research")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/jobs/" + jobId + "/research")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Atlassian"))
                .andExpect(jsonPath("$.briefing").isNotEmpty());
    }

    @Test
    void researchReplacesExistingRecord() throws Exception {
        when(claudeService.call(anyString(), anyString()))
                .thenReturn("## Company Overview\nFirst briefing.")
                .thenReturn("## Company Overview\nUpdated briefing.");

        mockMvc.perform(post("/api/jobs/" + jobId + "/research")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/jobs/" + jobId + "/research")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/jobs/" + jobId + "/research")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.briefing").value("## Company Overview\nUpdated briefing."));
    }
}
