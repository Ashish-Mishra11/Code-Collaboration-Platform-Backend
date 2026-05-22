package com.executionservice.repository;

import com.executionservice.entity.ExecutionJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ExecutionRepository extends JpaRepository<ExecutionJob, String> {

    Optional<ExecutionJob> findByJobId(String jobId);

    List<ExecutionJob> findByUserId(int userId);

    List<ExecutionJob> findByProjectId(int projectId);

    List<ExecutionJob> findByStatus(String status);

    List<ExecutionJob> findByLanguage(String language);

    List<ExecutionJob> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    long countByUserId(int userId);

    long countByUserIdAndStatus(int userId, String status);

}
