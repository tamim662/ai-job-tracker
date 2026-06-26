package com.jobtracker.service;

import com.jobtracker.dto.UpdateProfileRequest;
import com.jobtracker.dto.UserProfileDto;
import com.jobtracker.entity.UserProfile;
import com.jobtracker.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository repository;

    @Transactional
    public UserProfileDto getProfile() {
        UserProfile profile = repository.findById(1L)
                .orElseGet(() -> repository.save(new UserProfile()));
        return toDto(profile);
    }

    @Transactional
    public UserProfileDto updateProfile(UpdateProfileRequest req) {
        UserProfile profile = repository.findById(1L)
                .orElseGet(() -> repository.save(new UserProfile()));
        profile.setName(req.name());
        profile.setEmail(req.email());
        profile.setPhone(req.phone());
        profile.setLinkedinUrl(req.linkedinUrl());
        profile.setGithubUrl(req.githubUrl());
        profile.setTargetRoles(req.targetRoles());
        profile.setPreferredLocations(req.preferredLocations());
        profile.setVisaNote(req.visaNote());
        profile.setSalaryExpectation(req.salaryExpectation());
        profile.setAvailability(req.availability());
        profile.setDefaultHrEmail(req.defaultHrEmail());
        profile.setDefaultLinkedinMessage(req.defaultLinkedinMessage());
        return toDto(repository.save(profile));
    }

    private UserProfileDto toDto(UserProfile p) {
        return new UserProfileDto(
                p.getId(),
                p.getName(),
                p.getEmail(),
                p.getPhone(),
                p.getLinkedinUrl(),
                p.getGithubUrl(),
                p.getTargetRoles(),
                p.getPreferredLocations(),
                p.getVisaNote(),
                p.getSalaryExpectation(),
                p.getAvailability(),
                p.getDefaultHrEmail(),
                p.getDefaultLinkedinMessage()
        );
    }
}
