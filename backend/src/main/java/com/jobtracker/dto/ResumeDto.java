package com.jobtracker.dto;

import java.time.LocalDateTime;

public record ResumeDto(
        Long id,
        String fileName,
        String fileUrl,
        String parsedText,
        LocalDateTime createdAt
) {}
