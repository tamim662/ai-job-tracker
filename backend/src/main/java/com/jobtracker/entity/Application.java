package com.jobtracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "applications")
@Getter
@Setter
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long jobId;

    @Column(nullable = false)
    private String status;

    private LocalDate appliedDate;
    private String resumeVersion;
    private String coverLetterVersion;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private LocalDate followUpDate;
    private LocalDate interviewDate;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = "SAVED";
    }
}
