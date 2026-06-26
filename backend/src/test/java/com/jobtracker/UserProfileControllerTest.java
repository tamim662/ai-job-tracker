package com.jobtracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void obtainToken() throws Exception {
        Map<String, String> creds = Map.of("username", "admin", "password", "changeme");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creds)))
                .andReturn();
        token = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    @Test
    void getProfileWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getProfileWithTokenReturns200() throws Exception {
        mockMvc.perform(get("/api/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void updateProfilePersistsAllFields() throws Exception {
        Map<String, String> body = new java.util.HashMap<>(Map.of(
                "name", "Tamim Ahmed",
                "email", "tamim@example.com",
                "phone", "+61 400 000 000",
                "linkedinUrl", "https://linkedin.com/in/tamim",
                "githubUrl", "https://github.com/tamim",
                "targetRoles", "Software Engineer, Backend Developer",
                "preferredLocations", "Sydney, Melbourne",
                "visaNote", "Australian PR",
                "salaryExpectation", "120000-150000 AUD",
                "availability", "2 weeks notice"
        ));
        body.put("defaultHrEmail", "Dear Hiring Manager, I am writing to express my interest...");
        body.put("defaultLinkedinMessage", "Hi, I noticed your posting for [role] at [company]...");

        mockMvc.perform(put("/api/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tamim Ahmed"))
                .andExpect(jsonPath("$.email").value("tamim@example.com"))
                .andExpect(jsonPath("$.phone").value("+61 400 000 000"))
                .andExpect(jsonPath("$.linkedinUrl").value("https://linkedin.com/in/tamim"))
                .andExpect(jsonPath("$.githubUrl").value("https://github.com/tamim"))
                .andExpect(jsonPath("$.targetRoles").value("Software Engineer, Backend Developer"))
                .andExpect(jsonPath("$.preferredLocations").value("Sydney, Melbourne"))
                .andExpect(jsonPath("$.visaNote").value("Australian PR"))
                .andExpect(jsonPath("$.salaryExpectation").value("120000-150000 AUD"))
                .andExpect(jsonPath("$.availability").value("2 weeks notice"))
                .andExpect(jsonPath("$.defaultHrEmail").value("Dear Hiring Manager, I am writing to express my interest..."))
                .andExpect(jsonPath("$.defaultLinkedinMessage").value("Hi, I noticed your posting for [role] at [company]..."));
    }

    @Test
    void getAfterUpdateReturnsUpdatedValues() throws Exception {
        Map<String, String> body = Map.of(
                "name", "Jane Doe",
                "email", "jane@example.com",
                "phone", "",
                "linkedinUrl", "",
                "githubUrl", "",
                "targetRoles", "Data Engineer",
                "preferredLocations", "Brisbane",
                "visaNote", "",
                "salaryExpectation", "",
                "availability", "Immediate"
        );

        mockMvc.perform(put("/api/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jane Doe"))
                .andExpect(jsonPath("$.targetRoles").value("Data Engineer"))
                .andExpect(jsonPath("$.availability").value("Immediate"));
    }

    @Test
    void updateProfileWithoutTokenReturns401() throws Exception {
        Map<String, String> body = Map.of("name", "Hacker");
        mockMvc.perform(put("/api/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }
}
