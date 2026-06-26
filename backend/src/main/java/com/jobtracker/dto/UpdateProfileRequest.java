package com.jobtracker.dto;

public record UpdateProfileRequest(
        String name,
        String email,
        String phone,
        String linkedinUrl,
        String githubUrl,
        String targetRoles,
        String preferredLocations,
        String visaNote,
        String salaryExpectation,
        String availability,
        String defaultHrEmail,
        String defaultLinkedinMessage
) {}
