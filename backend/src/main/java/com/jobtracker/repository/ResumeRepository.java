package com.jobtracker.repository;

import com.jobtracker.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResumeRepository extends JpaRepository<Resume, Long> {
    List<Resume> findAllByOrderByCreatedAtDesc();
    Optional<Resume> findTopByOrderByCreatedAtDesc();
}
