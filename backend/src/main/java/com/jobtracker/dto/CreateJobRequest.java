package com.jobtracker.dto;

import java.time.LocalDate;

public record CreateJobRequest(
        String title,
        String company,
        String location,
        String platform,
        String jobUrl,
        String description,
        String salary,
        String jobType,
        LocalDate postedDate,
        LocalDate closingDate
) {}
