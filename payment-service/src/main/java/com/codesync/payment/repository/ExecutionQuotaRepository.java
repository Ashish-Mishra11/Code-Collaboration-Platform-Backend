package com.codesync.payment.repository;

import com.codesync.payment.entity.ExecutionQuota;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExecutionQuotaRepository extends JpaRepository<ExecutionQuota, Long> {

    Optional<ExecutionQuota> findByUserIdAndProjectIdAndFileId(
            Integer userId, Integer projectId, Integer fileId);

    long countByProjectId(Integer projectId);
}
