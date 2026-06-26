package com.jobtracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.dto.JobSearchPageDto;
import com.jobtracker.dto.JobSearchResultDto;
import com.jobtracker.util.KeywordMatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdzunaService {

    private static final String ADZUNA_BASE_URL = "https://api.adzuna.com/v1/api/jobs/au/search/";

    @Value("${app.adzuna.app-id}")
    private String appId;

    @Value("${app.adzuna.app-key}")
    private String appKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public JobSearchPageDto search(String what, String where, int resultsPerPage, int page, String resumeText) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(ADZUNA_BASE_URL + page)
                .queryParam("app_id", appId)
                .queryParam("app_key", appKey)
                .queryParam("results_per_page", resultsPerPage)
                .queryParam("what", what)
                .queryParam("content-type", "application/json");
        if (where != null && !where.isBlank()) {
            builder.queryParam("where", where);
        }
        URI uri = builder.build().encode().toUri();

        String response = restTemplate.getForObject(uri, String.class);

        List<JobSearchResultDto> results = new ArrayList<>();
        int totalCount = 0;
        try {
            JsonNode root = objectMapper.readTree(response);
            totalCount = root.path("count").asInt(0);
            for (JsonNode node : root.path("results")) {
                String title = node.path("title").asText();
                String description = node.path("description").asText();
                int matchScore = KeywordMatcher.score(title + " " + description, resumeText);

                results.add(new JobSearchResultDto(
                        node.path("id").asText(),
                        title,
                        node.at("/company/display_name").asText(),
                        node.at("/location/display_name").asText(),
                        description,
                        node.path("redirect_url").asText(),
                        node.has("salary_min") ? node.path("salary_min").asDouble() : null,
                        node.has("salary_max") ? node.path("salary_max").asDouble() : null,
                        node.path("contract_type").asText(null),
                        node.path("created").asText(null),
                        matchScore
                ));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Adzuna response", e);
        }
        int totalPages = resultsPerPage > 0 ? (int) Math.ceil((double) totalCount / resultsPerPage) : 1;
        return new JobSearchPageDto(results, totalCount, page, resultsPerPage, totalPages);
    }
}
