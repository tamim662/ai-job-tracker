package com.jobtracker.repository;

import com.jobtracker.entity.CoverLetter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CoverLetterRepository extends JpaRepository<CoverLetter, Long> {
    List<CoverLetter> findAllByOrderByCreatedAtDesc();
    Optional<CoverLetter> findTopByOrderByCreatedAtDesc();
}
