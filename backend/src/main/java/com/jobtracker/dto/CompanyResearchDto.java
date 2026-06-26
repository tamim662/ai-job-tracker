package com.jobtracker.dto;

import java.time.LocalDateTime;

public record CompanyResearchDto(
        Long id,
        Long jobId,
        String companyName,
        String briefing,
        LocalDateTime createdAt
) {}
