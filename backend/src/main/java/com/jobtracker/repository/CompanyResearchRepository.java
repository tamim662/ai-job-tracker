package com.jobtracker.repository;

import com.jobtracker.entity.CompanyResearch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyResearchRepository extends JpaRepository<CompanyResearch, Long> {
    Optional<CompanyResearch> findTopByJobIdOrderByCreatedAtDesc(Long jobId);
}
