package com.jobtracker.controller;

import com.jobtracker.dto.UpdateProfileRequest;
import com.jobtracker.dto.UserProfileDto;
import com.jobtracker.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService service;

    @GetMapping
    public ResponseEntity<UserProfileDto> getProfile() {
        return ResponseEntity.ok(service.getProfile());
    }

    @PutMapping
    public ResponseEntity<UserProfileDto> updateProfile(@RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(service.updateProfile(request));
    }
}
