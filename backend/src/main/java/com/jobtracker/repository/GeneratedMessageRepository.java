package com.jobtracker.repository;

import com.jobtracker.entity.GeneratedMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GeneratedMessageRepository extends JpaRepository<GeneratedMessage, Long> {
    List<GeneratedMessage> findAllByJobIdOrderByCreatedAtDesc(Long jobId);
}
