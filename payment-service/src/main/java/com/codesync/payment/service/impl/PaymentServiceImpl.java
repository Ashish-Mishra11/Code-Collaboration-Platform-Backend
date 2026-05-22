package com.codesync.payment.service.impl;

import com.codesync.payment.dto.QuotaResponseDto;
import com.codesync.payment.dto.TokenPurchaseRequest;
import com.codesync.payment.entity.ExecutionQuota;
import com.codesync.payment.entity.PaymentTransaction;
import com.codesync.payment.exception.DuplicateTransactionException;
import com.codesync.payment.exception.QuotaExceededException;
import com.codesync.payment.kafka.PaymentNotificationEvent;
import com.codesync.payment.kafka.PaymentNotificationPublisher;
import com.codesync.payment.repository.ExecutionQuotaRepository;
import com.codesync.payment.repository.PaymentTransactionRepository;
import com.codesync.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final int FREE_EXECUTION_LIMIT = 50;
    private static final BigDecimal PRICE_PER_100_TOKENS = new BigDecimal("500.00");

    private final ExecutionQuotaRepository quotaRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final PaymentNotificationPublisher notificationPublisher;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    // ─── Quota Management ─────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public QuotaResponseDto getQuota(Integer userId, Integer projectId, Integer fileId) {
        ExecutionQuota quota = getOrCreateQuota(userId, projectId, fileId);
        return mapToDto(quota);
    }

    @Override
    @Transactional
    public QuotaResponseDto consumeExecution(Integer userId, Integer projectId, Integer fileId) {
        ExecutionQuota quota = getOrCreateQuota(userId, projectId, fileId);

        if (!quota.canExecute()) {
            log.warn("Quota exceeded for user={} project={} file={} used={}",
                    userId, projectId, fileId, quota.getExecutionsUsed());
            throw new QuotaExceededException(
                    String.format("Execution limit reached. Used %d / %d. Please purchase more tokens.",
                            quota.getExecutionsUsed(), quota.totalAllowed()));
        }

        quota.setExecutionsUsed(quota.getExecutionsUsed() + 1);
        ExecutionQuota saved = quotaRepository.save(quota);
        log.info("Execution consumed: user={} file={} used={} remaining={}",
                userId, fileId, saved.getExecutionsUsed(), saved.remaining());
        return mapToDto(saved);
    }

    // ─── Token Purchase ───────────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentTransaction purchaseTokens(TokenPurchaseRequest request) {
        // Idempotency check
        if (request.getGatewayReference() != null && !request.getGatewayReference().isBlank()) {
            transactionRepository.findByGatewayReference(request.getGatewayReference())
                    .ifPresent(t -> {
                        throw new DuplicateTransactionException(
                                "Transaction already processed: " + request.getGatewayReference());
                    });
        }

        int tokens = request.getTokensToBuy();
        BigDecimal amount = PRICE_PER_100_TOKENS
                .multiply(BigDecimal.valueOf(tokens))
                .divide(BigDecimal.valueOf(100));

        // Save transaction as PENDING
        PaymentTransaction tx = PaymentTransaction.builder()
                .userId(request.getUserId())
                .projectId(request.getProjectId())
                .fileId(request.getFileId())
                .tokensPurchased(tokens)
                .amount(amount)
                .gatewayReference(request.getGatewayReference())
                .status("PENDING")
                .build();
        tx = transactionRepository.save(tx);

        try {
            // Credit tokens to quota
            ExecutionQuota quota = getOrCreateQuota(
                    request.getUserId(), request.getProjectId(), request.getFileId());
            quota.setPurchasedTokens(quota.getPurchasedTokens() + tokens);
            quotaRepository.save(quota);

            // Mark transaction SUCCESS
            tx.setStatus("SUCCESS");
            tx.setCompletedAt(LocalDateTime.now());
            tx.setRemarks("Credited " + tokens + " tokens. New balance: " + quota.remaining());
            tx = transactionRepository.save(tx);

            log.info("Tokens purchased: user={} file={} tokens={} gatewayRef={}",
                    request.getUserId(), request.getFileId(), tokens, request.getGatewayReference());

            // ── Publish SUCCESS event to Kafka ─────────────────────────────
            publishPaymentEvent(tx, request, "SUCCESS",
                    "Credited " + tokens + " tokens. New balance: " + quota.remaining());

        } catch (DuplicateTransactionException | QuotaExceededException e) {
            throw e; // re-throw known domain exceptions without Kafka publish

        } catch (Exception e) {
            // Mark transaction FAILED
            tx.setStatus("FAILED");
            tx.setRemarks("Error: " + e.getMessage());
            transactionRepository.save(tx);
            log.error("Token purchase failed for user={}", request.getUserId(), e);

            // ── Publish FAILED event to Kafka ──────────────────────────────
            publishPaymentEvent(tx, request, "FAILED", "Error: " + e.getMessage());

            throw e;
        }

        return tx;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentTransaction> getTransactionsByUser(Integer userId) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public com.codesync.payment.dto.RazorpayOrderResponse createRazorpayOrder(
            com.codesync.payment.dto.RazorpayOrderRequest request) {
        try {
            RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", request.getAmount());
            orderRequest.put("currency", request.getCurrency());
            orderRequest.put("receipt", "order_rcptid_" + System.currentTimeMillis());

            Order order = razorpay.orders.create(orderRequest);

            return com.codesync.payment.dto.RazorpayOrderResponse.builder()
                    .orderId(order.get("id"))
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .build();

        } catch (Exception e) {
            log.error("Error creating Razorpay order: ", e);
            throw new RuntimeException("Failed to create Razorpay Order: " + e.getMessage());
        }
    }

    // ─── Internal helpers ─────────────────────────────────────────────────────

    private ExecutionQuota getOrCreateQuota(Integer userId, Integer projectId, Integer fileId) {
        return quotaRepository.findByUserIdAndProjectIdAndFileId(userId, projectId, fileId)
                .orElseGet(() -> {
                    ExecutionQuota q = ExecutionQuota.builder()
                            .userId(userId)
                            .projectId(projectId)
                            .fileId(fileId)
                            .freeLimit(FREE_EXECUTION_LIMIT)
                            .executionsUsed(0)
                            .purchasedTokens(0)
                            .build();
                    ExecutionQuota saved = quotaRepository.save(q);
                    log.info("Created new quota: user={} project={} file={} freeLimit={}",
                            userId, projectId, fileId, FREE_EXECUTION_LIMIT);
                    return saved;
                });
    }

    private QuotaResponseDto mapToDto(ExecutionQuota q) {
        return QuotaResponseDto.builder()
                .userId(q.getUserId())
                .projectId(q.getProjectId())
                .fileId(q.getFileId())
                .executionsUsed(q.getExecutionsUsed())
                .freeLimit(q.getFreeLimit())
                .purchasedTokens(q.getPurchasedTokens())
                .totalAllowed(q.totalAllowed())
                .remaining(q.remaining())
                .canExecute(q.canExecute())
                .build();
    }

    /**
     * Builds and fires a {@link PaymentNotificationEvent} to Kafka.
     * Any Kafka failure is swallowed — the payment outcome must not be affected.
     */
    private void publishPaymentEvent(PaymentTransaction tx,
                                     TokenPurchaseRequest request,
                                     String status,
                                     String remarks) {
        try {
            if (request.getEmail() == null || request.getEmail().isBlank()) {
                log.warn("[PaymentKafka] Skipping notification — email is missing for user={}. " +
                        "Ensure the frontend passes 'email' in the TokenPurchaseRequest.", request.getUserId());
                return;
            }

            String displayName = (request.getUserName() != null && !request.getUserName().isBlank())
                    ? request.getUserName()
                    : "Developer-" + request.getUserId();

            PaymentNotificationEvent event = PaymentNotificationEvent.builder()
                    .userId(request.getUserId())
                    .email(request.getEmail())          // comes from frontend / API caller
                    .userName(displayName)
                    .tokensPurchased(tx.getTokensPurchased())
                    .amount(tx.getAmount())
                    .gatewayReference(tx.getGatewayReference())
                    .status(status)
                    .remarks(remarks)
                    .transactionTime(LocalDateTime.now())
                    .build();

            notificationPublisher.publishPaymentNotification(event);

        } catch (Exception ex) {
            log.warn("[PaymentKafka] Could not build/publish payment event: {}", ex.getMessage());
        }
    }
}

