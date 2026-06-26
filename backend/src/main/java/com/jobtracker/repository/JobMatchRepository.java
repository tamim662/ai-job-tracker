package com.jobtracker.repository;

import com.jobtracker.entity.JobMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobMatchRepository extends JpaRepository<JobMatch, Long> {
    Optional<JobMatch> findTopByJobIdOrderByCreatedAtDesc(Long jobId);
}
