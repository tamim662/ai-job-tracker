package com.jobtracker.dto;

import java.time.LocalDateTime;

public record GeneratedMessageDto(
        Long id,
        Long jobId,
        String type,
        String content,
        LocalDateTime createdAt
) {}
