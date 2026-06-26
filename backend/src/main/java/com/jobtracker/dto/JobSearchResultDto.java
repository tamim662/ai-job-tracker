package com.jobtracker.dto;

public record JobSearchResultDto(
        String externalId,
        String title,
        String company,
        String location,
        String description,
        String url,
        Double salaryMin,
        Double salaryMax,
        String contractType,
        String postedDate,
        Integer matchScore
) {}
