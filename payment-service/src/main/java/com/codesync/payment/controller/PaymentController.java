package com.codesync.payment.controller;

import com.codesync.payment.dto.ApiResponse;
import com.codesync.payment.dto.ExecutionCheckRequest;
import com.codesync.payment.dto.QuotaResponseDto;
import com.codesync.payment.dto.TokenPurchaseRequest;
import com.codesync.payment.entity.PaymentTransaction;
import com.codesync.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // ─── Quota ────────────────────────────────────────────────────────────────

    /**
     * GET /api/payment/quota?userId=&projectId=&fileId=
     * Returns the current quota for a user/project/file (creates it if first time).
     */
    @GetMapping("/quota")
    public ResponseEntity<ApiResponse<QuotaResponseDto>> getQuota(
            @RequestParam Integer userId,
            @RequestParam Integer projectId,
            @RequestParam Integer fileId) {
        QuotaResponseDto dto = paymentService.getQuota(userId, projectId, fileId);
        return ResponseEntity.ok(ApiResponse.success("Quota fetched", dto));
    }

    /**
     * POST /api/payment/quota/consume
     * Consumes one execution slot. Returns 402 if quota exceeded.
     */
    @PostMapping("/quota/consume")
    public ResponseEntity<ApiResponse<QuotaResponseDto>> consumeExecution(
            @Valid @RequestBody ExecutionCheckRequest request) {
        QuotaResponseDto dto = paymentService.consumeExecution(
                request.getUserId(), request.getProjectId(), request.getFileId());
        return ResponseEntity.ok(ApiResponse.success("Execution slot consumed", dto));
    }

    // ─── Token Purchase ───────────────────────────────────────────────────────

    /**
     * POST /api/payment/tokens/purchase
     * Purchase additional execution tokens.
     * Body: { userId, projectId, fileId, tokensToBuy, gatewayReference }
     */
    @PostMapping("/tokens/purchase")
    public ResponseEntity<ApiResponse<PaymentTransaction>> purchaseTokens(
            @Valid @RequestBody TokenPurchaseRequest request) {
        PaymentTransaction tx = paymentService.purchaseTokens(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tokens purchased successfully", tx));
    }

    // ─── Razorpay Integration ─────────────────────────────────────────────────

    /**
     * POST /api/payment/razorpay/create-order
     * Generates a Razorpay Order ID to securely initialize the frontend checkout.
     */
    @PostMapping("/razorpay/create-order")
    public ResponseEntity<ApiResponse<com.codesync.payment.dto.RazorpayOrderResponse>> createRazorpayOrder(
            @Valid @RequestBody com.codesync.payment.dto.RazorpayOrderRequest request) {
        com.codesync.payment.dto.RazorpayOrderResponse response = paymentService.createRazorpayOrder(request);
        return ResponseEntity.ok(ApiResponse.success("Order created successfully", response));
    }

    /**
     * GET /api/payment/transactions/{userId}
     * Get all payment history for a user.
     */
    @GetMapping("/transactions/{userId}")
    public ResponseEntity<ApiResponse<List<PaymentTransaction>>> getTransactions(
            @PathVariable Integer userId) {
        List<PaymentTransaction> txList = paymentService.getTransactionsByUser(userId);
        return ResponseEntity.ok(ApiResponse.success("Transactions fetched", txList));
    }

    /** Health check */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Payment service is running");
    }
}
