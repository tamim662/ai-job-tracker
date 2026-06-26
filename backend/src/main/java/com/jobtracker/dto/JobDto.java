package com.jobtracker.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record JobDto(
        Long id,
        String title,
        String company,
        String location,
        String platform,
        String jobUrl,
        String description,
        String salary,
        String jobType,
        LocalDate postedDate,
        LocalDate closingDate,
        LocalDateTime savedDate,
        Long applicationId,
        String applicationStatus
) {}
