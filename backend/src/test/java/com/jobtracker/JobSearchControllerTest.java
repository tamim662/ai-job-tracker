package com.jobtracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.dto.JobSearchPageDto;
import com.jobtracker.dto.JobSearchResultDto;
import com.jobtracker.dto.WebJobResultDto;
import com.jobtracker.service.AdzunaService;
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
class JobSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdzunaService adzunaService;

    @MockBean
    private TavilyService tavilyService;

    private String token;

    @BeforeEach
    void obtainToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "admin", "password", "changeme"))))
                .andReturn();
        token = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();

        JobSearchResultDto job = new JobSearchResultDto(
                "12345", "Java Developer", "TechCorp", "Sydney, NSW",
                "We need a Java developer with Spring Boot experience.",
                "https://example.com/job/12345",
                90000.0, 120000.0, "permanent", "2026-01-15T00:00:00Z",
                72
        );
        when(adzunaService.search(anyString(), anyString(), anyInt(), anyInt(), anyString()))
                .thenReturn(new JobSearchPageDto(List.of(job), 45, 1, 10, 5));

        when(tavilyService.searchJobs(anyString(), anyString(), anyInt(), anyString())).thenReturn(List.of(
                new WebJobResultDto(
                        "Senior Java Developer — Atlassian",
                        "https://www.seek.com.au/job/123",
                        "Atlassian is looking for a Senior Java Developer to join the platform team...",
                        "seek.com.au",
                        65
                )
        ));
    }

    @Test
    void searchWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/job-search").param("what", "java developer"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void searchMissingWhatReturns400() throws Exception {
        mockMvc.perform(get("/api/job-search")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void searchReturnsPagedResults() throws Exception {
        mockMvc.perform(get("/api/job-search")
                        .header("Authorization", "Bearer " + token)
                        .param("what", "java developer")
                        .param("where", "sydney"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.results[0].title").value("Java Developer"))
                .andExpect(jsonPath("$.results[0].company").value("TechCorp"))
                .andExpect(jsonPath("$.results[0].matchScore").value(72))
                .andExpect(jsonPath("$.totalCount").value(45))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalPages").value(5));
    }

    @Test
    void searchWithOnlyWhatParam() throws Exception {
        mockMvc.perform(get("/api/job-search")
                        .header("Authorization", "Bearer " + token)
                        .param("what", "backend engineer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray());
    }

    @Test
    void searchWithPageParam() throws Exception {
        mockMvc.perform(get("/api/job-search")
                        .header("Authorization", "Bearer " + token)
                        .param("what", "java developer")
                        .param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray());
    }

    @Test
    void webSearchWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/job-search/web").param("what", "java developer"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void webSearchMissingWhatReturns400() throws Exception {
        mockMvc.perform(get("/api/job-search/web")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void webSearchReturnsResults() throws Exception {
        mockMvc.perform(get("/api/job-search/web")
                        .header("Authorization", "Bearer " + token)
                        .param("what", "java developer")
                        .param("where", "sydney"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("Senior Java Developer — Atlassian"))
                .andExpect(jsonPath("$[0].source").value("seek.com.au"))
                .andExpect(jsonPath("$[0].matchScore").value(65));
    }
}
