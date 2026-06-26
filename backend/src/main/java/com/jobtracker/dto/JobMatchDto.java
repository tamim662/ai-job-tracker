package com.jobtracker.dto;

import java.time.LocalDateTime;

public record JobMatchDto(
        Long id,
        Long jobId,
        Long resumeId,
        Integer atsScore,
        String matchedSkills,
        String missingSkills,
        String suggestedSummary,
        String suggestedSkills,
        String suggestedExperience,
        LocalDateTime createdAt
) {}
