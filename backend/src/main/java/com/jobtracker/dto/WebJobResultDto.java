package com.jobtracker.dto;

public record WebJobResultDto(
        String title,
        String url,
        String snippet,
        String source,
        Integer matchScore
) {}
