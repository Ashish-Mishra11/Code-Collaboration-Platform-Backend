package com.codesync.payment.repository;

import com.codesync.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    List<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(Integer userId);
    List<PaymentTransaction> findByUserIdAndProjectIdAndFileId(Integer userId, Integer projectId, Integer fileId);
    Optional<PaymentTransaction> findByGatewayReference(String gatewayReference);
    long countByUserIdAndStatus(Integer userId, String status);
}
