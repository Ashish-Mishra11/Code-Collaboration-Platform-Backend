package com.codesync.payment.service;

import com.codesync.payment.dto.QuotaResponseDto;
import com.codesync.payment.dto.TokenPurchaseRequest;
import com.codesync.payment.entity.PaymentTransaction;

import java.util.List;

public interface PaymentService {

    /**
     * Returns quota details for a user/project/file combination.
     * Creates a quota record with 50 free executions if it doesn't exist.
     */
    QuotaResponseDto getQuota(Integer userId, Integer projectId, Integer fileId);

    /**
     * Attempts to consume one execution slot.
     * Returns the updated quota. Throws {@link com.codesync.payment.exception.QuotaExceededException}
     * if the user has no remaining executions.
     */
    QuotaResponseDto consumeExecution(Integer userId, Integer projectId, Integer fileId);

    /**
     * Purchases additional execution tokens and credits them to the quota.
     * Verifies the payment gateway reference is unique (idempotency).
     * Returns the completed payment transaction.
     */
    PaymentTransaction purchaseTokens(TokenPurchaseRequest request);

    /**
     * Returns all payment transactions for a user.
     */
    List<PaymentTransaction> getTransactionsByUser(Integer userId);

    /**
     * Creates a Razorpay order and returns the order ID.
     */
    com.codesync.payment.dto.RazorpayOrderResponse createRazorpayOrder(com.codesync.payment.dto.RazorpayOrderRequest request);
}
