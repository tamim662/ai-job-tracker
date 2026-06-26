package com.jobtracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.dto.WebJobResultDto;
import com.jobtracker.util.KeywordMatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TavilyService {

    private static final String TAVILY_URL = "https://api.tavily.com/search";

    private static final List<String> JOB_DOMAINS = List.of(
            "seek.com.au", "linkedin.com", "indeed.com.au", "jora.com",
            "careerone.com.au", "glassdoor.com.au", "adzuna.com.au",
            "ethicaljobs.com.au", "gradconnection.com.au", "workforceaustralia.gov.au"
    );

    @Value("${app.tavily.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public List<String> search(String query, int maxResults) {
        String response = callTavily(query, maxResults, null, "basic");
        List<String> contents = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(response);
            for (JsonNode node : root.path("results")) {
                String title = node.path("title").asText("");
                String content = node.path("content").asText("");
                if (!content.isBlank()) {
                    contents.add("[" + title + "]\n" + content);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Tavily response", e);
        }
        return contents;
    }

    public List<WebJobResultDto> searchJobs(String what, String where, int maxResults, String resumeText) {
        String query = what + " jobs" + (where != null && !where.isBlank() ? " " + where : "") + " australia";
        String response = callTavily(query, maxResults, JOB_DOMAINS, "advanced");

        List<WebJobResultDto> results = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(response);
            for (JsonNode node : root.path("results")) {
                String title = node.path("title").asText("").trim();
                String url = node.path("url").asText("").trim();
                String content = node.path("content").asText("").trim();
                if (title.isBlank() || url.isBlank()) continue;

                String snippet = content.length() > 300 ? content.substring(0, 300) + "…" : content;
                String source = extractDomain(url);
                int matchScore = KeywordMatcher.score(title + " " + content, resumeText);

                results.add(new WebJobResultDto(title, url, snippet, source, matchScore));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Tavily job search response", e);
        }
        return results;
    }

    private String callTavily(String query, int maxResults, List<String> includeDomains, String depth) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = includeDomains != null
                ? Map.of("query", query, "search_depth", depth, "max_results", maxResults, "include_domains", includeDomains)
                : Map.of("query", query, "search_depth", depth, "max_results", maxResults);

        return restTemplate.postForObject(TAVILY_URL, new HttpEntity<>(body, headers), String.class);
    }

    private String extractDomain(String url) {
        try {
            String host = new URI(url).getHost();
            if (host == null) return url;
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (Exception e) {
            return url;
        }
    }
}
