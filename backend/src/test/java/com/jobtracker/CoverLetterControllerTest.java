package com.jobtracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.service.FileParserService;
import com.jobtracker.service.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CoverLetterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private S3Service s3Service;

    @MockBean
    private FileParserService fileParserService;

    private String token;

    @BeforeEach
    void setup() throws Exception {
        when(fileParserService.parse(any())).thenReturn("Parsed cover letter text");
        when(s3Service.upload(anyString(), anyString(), any(), anyLong(), anyString()))
                .thenReturn("https://bucket.s3.ap-southeast-2.amazonaws.com/cover-letters/uuid_cover.pdf");
        doNothing().when(s3Service).delete(anyString());

        Map<String, String> creds = Map.of("username", "admin", "password", "changeme");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creds)))
                .andReturn();
        token = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void uploadWithoutTokenReturns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "cover.pdf", "application/pdf", "pdf content".getBytes());
        mockMvc.perform(multipart("/api/cover-letters").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadValidFileReturns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "cover.pdf", "application/pdf", "pdf content".getBytes());
        mockMvc.perform(multipart("/api/cover-letters").file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.fileName").value("cover.pdf"))
                .andExpect(jsonPath("$.parsedText").value("Parsed cover letter text"))
                .andExpect(jsonPath("$.fileUrl").isNotEmpty());
    }

    @Test
    void uploadEmptyFileReturns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "cover.pdf", "application/pdf", new byte[0]);
        mockMvc.perform(multipart("/api/cover-letters").file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listCoverLettersReturnsUploadedFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "my-cover.pdf", "application/pdf", "pdf bytes".getBytes());
        mockMvc.perform(multipart("/api/cover-letters").file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/cover-letters").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].fileName").value("my-cover.pdf"));
    }

    @Test
    void deleteCoverLetterReturns204() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "delete-me.pdf", "application/pdf", "pdf".getBytes());
        MvcResult uploadResult = mockMvc.perform(multipart("/api/cover-letters").file(file)
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        Long id = objectMapper.readTree(uploadResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/cover-letters/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteNonExistentCoverLetterReturns404() throws Exception {
        mockMvc.perform(delete("/api/cover-letters/99999").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
