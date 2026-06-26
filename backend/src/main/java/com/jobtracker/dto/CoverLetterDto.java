package com.jobtracker.dto;

import java.time.LocalDateTime;

public record CoverLetterDto(
        Long id,
        String fileName,
        String fileUrl,
        String parsedText,
        LocalDateTime createdAt
) {}
