package com.jobtracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "jobs")
@Getter
@Setter
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String company;
    private String location;
    private String platform;

    @Column(length = 1000)
    private String jobUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String salary;
    private String jobType;
    private LocalDate postedDate;
    private LocalDate closingDate;

    @Column(updatable = false)
    private LocalDateTime savedDate;

    @PrePersist
    void onCreate() {
        savedDate = LocalDateTime.now();
    }
}
