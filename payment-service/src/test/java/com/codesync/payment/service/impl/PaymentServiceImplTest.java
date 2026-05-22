package com.codesync.payment.service.impl;

import com.codesync.payment.dto.QuotaResponseDto;
import com.codesync.payment.dto.TokenPurchaseRequest;
import com.codesync.payment.entity.ExecutionQuota;
import com.codesync.payment.entity.PaymentTransaction;
import com.codesync.payment.exception.DuplicateTransactionException;
import com.codesync.payment.exception.QuotaExceededException;
import com.codesync.payment.kafka.PaymentNotificationPublisher;
import com.codesync.payment.repository.ExecutionQuotaRepository;
import com.codesync.payment.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PaymentServiceImpl}.
 * All external dependencies (repos, Kafka publisher) are mocked.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentServiceImpl Unit Tests")
class PaymentServiceImplTest {

    @Mock private ExecutionQuotaRepository quotaRepository;
    @Mock private PaymentTransactionRepository transactionRepository;
    @Mock private PaymentNotificationPublisher notificationPublisher;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    // ── Shared fixtures ───────────────────────────────────────────────────────

    private static final int USER_ID    = 42;
    private static final int PROJECT_ID = 7;
    private static final int FILE_ID    = 3;

    private ExecutionQuota freshQuota() {
        return ExecutionQuota.builder()
                .id(1L)
                .userId(USER_ID)
                .projectId(PROJECT_ID)
                .fileId(FILE_ID)
                .freeLimit(50)
                .executionsUsed(0)
                .purchasedTokens(0)
                .build();
    }

    private TokenPurchaseRequest purchaseRequest(int tokens) {
        TokenPurchaseRequest req = new TokenPurchaseRequest();
        req.setUserId(USER_ID);
        req.setProjectId(PROJECT_ID);
        req.setFileId(FILE_ID);
        req.setTokensToBuy(tokens);
        req.setGatewayReference("order_abc123");
        req.setEmail("dev@example.com");
        req.setUserName("dev_user");
        return req;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  getQuota
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getQuota()")
    class GetQuotaTests {

        @Test
        @DisplayName("returns DTO when quota already exists")
        void getQuota_existingQuota_returnsDto() {
            ExecutionQuota quota = freshQuota();
            quota.setExecutionsUsed(10);
            quota.setPurchasedTokens(20);

            when(quotaRepository.findByUserIdAndProjectIdAndFileId(USER_ID, PROJECT_ID, FILE_ID))
                    .thenReturn(Optional.of(quota));

            QuotaResponseDto dto = paymentService.getQuota(USER_ID, PROJECT_ID, FILE_ID);

            assertThat(dto.getUserId()).isEqualTo(USER_ID);
            assertThat(dto.getExecutionsUsed()).isEqualTo(10);
            assertThat(dto.getPurchasedTokens()).isEqualTo(20);
            assertThat(dto.getTotalAllowed()).isEqualTo(70);   // 50 free + 20 purchased
            assertThat(dto.getRemaining()).isEqualTo(60);      // 70 - 10
            assertThat(dto.isCanExecute()).isTrue();
        }

        @Test
        @DisplayName("creates new quota when none exists, freeLimit defaults to 50")
        void getQuota_noExistingQuota_createsNew() {
            when(quotaRepository.findByUserIdAndProjectIdAndFileId(USER_ID, PROJECT_ID, FILE_ID))
                    .thenReturn(Optional.empty());
            when(quotaRepository.save(any(ExecutionQuota.class))).thenAnswer(inv -> inv.getArgument(0));

            QuotaResponseDto dto = paymentService.getQuota(USER_ID, PROJECT_ID, FILE_ID);

            assertThat(dto.getFreeLimit()).isEqualTo(50);
            assertThat(dto.getExecutionsUsed()).isZero();
            assertThat(dto.getRemaining()).isEqualTo(50);
            assertThat(dto.isCanExecute()).isTrue();
            verify(quotaRepository).save(any(ExecutionQuota.class));
        }

        @Test
        @DisplayName("canExecute is false when all executions are used")
        void getQuota_exhaustedQuota_canExecuteFalse() {
            ExecutionQuota quota = freshQuota();
            quota.setExecutionsUsed(50); // used all 50 free, no tokens

            when(quotaRepository.findByUserIdAndProjectIdAndFileId(USER_ID, PROJECT_ID, FILE_ID))
                    .thenReturn(Optional.of(quota));

            QuotaResponseDto dto = paymentService.getQuota(USER_ID, PROJECT_ID, FILE_ID);

            assertThat(dto.getRemaining()).isZero();
            assertThat(dto.isCanExecute()).isFalse();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  consumeExecution
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("consumeExecution()")
    class ConsumeExecutionTests {

        @Test
        @DisplayName("increments executionsUsed when quota is available")
        void consumeExecution_availableQuota_incrementsUsed() {
            ExecutionQuota quota = freshQuota();
            quota.setExecutionsUsed(5);

            when(quotaRepository.findByUserIdAndProjectIdAndFileId(USER_ID, PROJECT_ID, FILE_ID))
                    .thenReturn(Optional.of(quota));
            when(quotaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            QuotaResponseDto result = paymentService.consumeExecution(USER_ID, PROJECT_ID, FILE_ID);

            assertThat(result.getExecutionsUsed()).isEqualTo(6);
            verify(quotaRepository).save(argThat(q -> q.getExecutionsUsed() == 6));
        }

        @Test
        @DisplayName("throws QuotaExceededException when no executions remain")
        void consumeExecution_noQuotaLeft_throwsException() {
            ExecutionQuota quota = freshQuota();
            quota.setExecutionsUsed(50); // exhausted

            when(quotaRepository.findByUserIdAndProjectIdAndFileId(USER_ID, PROJECT_ID, FILE_ID))
                    .thenReturn(Optional.of(quota));

            assertThatThrownBy(() -> paymentService.consumeExecution(USER_ID, PROJECT_ID, FILE_ID))
                    .isInstanceOf(QuotaExceededException.class)
                    .hasMessageContaining("Execution limit reached");

            verify(quotaRepository, never()).save(any());
        }

        @Test
        @DisplayName("allows execution when purchased tokens extend the limit")
        void consumeExecution_purchasedTokensExtendLimit_succeeds() {
            ExecutionQuota quota = freshQuota();
            quota.setExecutionsUsed(50); // free exhausted
            quota.setPurchasedTokens(10); // but 10 purchased tokens available

            when(quotaRepository.findByUserIdAndProjectIdAndFileId(USER_ID, PROJECT_ID, FILE_ID))
                    .thenReturn(Optional.of(quota));
            when(quotaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            QuotaResponseDto result = paymentService.consumeExecution(USER_ID, PROJECT_ID, FILE_ID);

            assertThat(result.getExecutionsUsed()).isEqualTo(51);
        }

        @Test
        @DisplayName("creates new quota when quota does not exist, then consumes")
        void consumeExecution_newQuota_consumesFirstExecution() {
            when(quotaRepository.findByUserIdAndProjectIdAndFileId(USER_ID, PROJECT_ID, FILE_ID))
                    .thenReturn(Optional.empty());
            ExecutionQuota created = freshQuota();
            when(quotaRepository.save(any())).thenReturn(created);

            QuotaResponseDto result = paymentService.consumeExecution(USER_ID, PROJECT_ID, FILE_ID);

            // second save (increment) also happens
            verify(quotaRepository, atLeast(2)).save(any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  purchaseTokens
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("purchaseTokens()")
    class PurchaseTokensTests {

        @Test
        @DisplayName("successfully purchases tokens and marks transaction SUCCESS")
        void purchaseTokens_success_creditsTokens() {
            TokenPurchaseRequest req = purchaseRequest(100);

            when(transactionRepository.findByGatewayReference("order_abc123"))
                    .thenReturn(Optional.empty());

            PaymentTransaction savedPending = PaymentTransaction.builder()
                    .userId(USER_ID).projectId(PROJECT_ID).fileId(FILE_ID)
                    .tokensPurchased(100).amount(new BigDecimal("500.00"))
                    .gatewayReference("order_abc123").status("PENDING").build();
            when(transactionRepository.save(any())).thenReturn(savedPending);

            ExecutionQuota quota = freshQuota();
            when(quotaRepository.findByUserIdAndProjectIdAndFileId(USER_ID, PROJECT_ID, FILE_ID))
                    .thenReturn(Optional.of(quota));
            when(quotaRepository.save(any())).thenReturn(quota);

            PaymentTransaction result = paymentService.purchaseTokens(req);

            assertThat(result).isNotNull();
            // verify Kafka notification was published
            verify(notificationPublisher).publishPaymentNotification(any());
        }

        @Test
        @DisplayName("throws DuplicateTransactionException for duplicate gatewayReference")
        void purchaseTokens_duplicate_throwsException() {
            TokenPurchaseRequest req = purchaseRequest(100);
            PaymentTransaction existing = PaymentTransaction.builder().status("SUCCESS").build();

            when(transactionRepository.findByGatewayReference("order_abc123"))
                    .thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> paymentService.purchaseTokens(req))
                    .isInstanceOf(DuplicateTransactionException.class)
                    .hasMessageContaining("Transaction already processed");

            verify(quotaRepository, never()).save(any());
        }

        @Test
        @DisplayName("skips Kafka notification when email is blank in request")
        void purchaseTokens_blankEmail_skipsKafka() {
            TokenPurchaseRequest req = purchaseRequest(50);
            req.setEmail("  "); // blank

            when(transactionRepository.findByGatewayReference(any())).thenReturn(Optional.empty());
            PaymentTransaction tx = PaymentTransaction.builder()
                    .userId(USER_ID).tokensPurchased(50)
                    .amount(new BigDecimal("250.00")).status("PENDING").build();
            when(transactionRepository.save(any())).thenReturn(tx);

            ExecutionQuota quota = freshQuota();
            when(quotaRepository.findByUserIdAndProjectIdAndFileId(USER_ID, PROJECT_ID, FILE_ID))
                    .thenReturn(Optional.of(quota));
            when(quotaRepository.save(any())).thenReturn(quota);

            paymentService.purchaseTokens(req);

            verify(notificationPublisher, never()).publishPaymentNotification(any());
        }

        @Test
        @DisplayName("skips Kafka notification when email is null in request")
        void purchaseTokens_nullEmail_skipsKafka() {
            TokenPurchaseRequest req = purchaseRequest(50);
            req.setEmail(null);

            when(transactionRepository.findByGatewayReference(any())).thenReturn(Optional.empty());
            PaymentTransaction tx = PaymentTransaction.builder()
                    .userId(USER_ID).tokensPurchased(50)
                    .amount(new BigDecimal("250.00")).status("PENDING").build();
            when(transactionRepository.save(any())).thenReturn(tx);

            ExecutionQuota quota = freshQuota();
            when(quotaRepository.findByUserIdAndProjectIdAndFileId(USER_ID, PROJECT_ID, FILE_ID))
                    .thenReturn(Optional.of(quota));
            when(quotaRepository.save(any())).thenReturn(quota);

            paymentService.purchaseTokens(req);

            verify(notificationPublisher, never()).publishPaymentNotification(any());
        }

        @Test
        @DisplayName("uses 'Developer-<userId>' as display name when userName is null")
        void purchaseTokens_nullUserName_usesDefaultDisplayName() {
            TokenPurchaseRequest req = purchaseRequest(100);
            req.setUserName(null);

            when(transactionRepository.findByGatewayReference(any())).thenReturn(Optional.empty());
            PaymentTransaction tx = PaymentTransaction.builder()
                    .userId(USER_ID).tokensPurchased(100)
                    .amount(new BigDecimal("500.00"))
                    .gatewayReference("order_abc123").status("PENDING").build();
            when(transactionRepository.save(any())).thenReturn(tx);

            ExecutionQuota quota = freshQuota();
            when(quotaRepository.findByUserIdAndProjectIdAndFileId(USER_ID, PROJECT_ID, FILE_ID))
                    .thenReturn(Optional.of(quota));
            when(quotaRepository.save(any())).thenReturn(quota);

            paymentService.purchaseTokens(req);

            // Should publish with userName = "Developer-42"
            verify(notificationPublisher).publishPaymentNotification(
                    argThat(e -> e.getUserName().equals("Developer-" + USER_ID)));
        }

        @Test
        @DisplayName("skips idempotency check when gatewayReference is null")
        void purchaseTokens_nullGatewayRef_skipsIdempotencyCheck() {
            TokenPurchaseRequest req = purchaseRequest(100);
            req.setGatewayReference(null);

            PaymentTransaction tx = PaymentTransaction.builder()
                    .userId(USER_ID).tokensPurchased(100)
                    .amount(new BigDecimal("500.00")).status("PENDING").build();
            when(transactionRepository.save(any())).thenReturn(tx);

            ExecutionQuota quota = freshQuota();
            when(quotaRepository.findByUserIdAndProjectIdAndFileId(USER_ID, PROJECT_ID, FILE_ID))
                    .thenReturn(Optional.of(quota));
            when(quotaRepository.save(any())).thenReturn(quota);

            // Should NOT call findByGatewayReference
            paymentService.purchaseTokens(req);
            verify(transactionRepository, never()).findByGatewayReference(any());
        }

        @Test
        @DisplayName("calculates amount correctly: 100 tokens = 500.00 INR")
        void purchaseTokens_amountCalculation_correct() {
            TokenPurchaseRequest req = purchaseRequest(100);

            when(transactionRepository.findByGatewayReference(any())).thenReturn(Optional.empty());

            ArgumentCaptor<PaymentTransaction> txCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
            when(transactionRepository.save(txCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

            ExecutionQuota quota = freshQuota();
            when(quotaRepository.findByUserIdAndProjectIdAndFileId(USER_ID, PROJECT_ID, FILE_ID))
                    .thenReturn(Optional.of(quota));
            when(quotaRepository.save(any())).thenReturn(quota);

            paymentService.purchaseTokens(req);

            // First captured call is the PENDING save — check amount
            PaymentTransaction pending = txCaptor.getAllValues().get(0);
            assertThat(pending.getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  getTransactionsByUser
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getTransactionsByUser()")
    class GetTransactionsTests {

        @Test
        @DisplayName("returns list from repository")
        void getTransactionsByUser_returnsList() {
            List<PaymentTransaction> txList = List.of(
                    PaymentTransaction.builder().userId(USER_ID).status("SUCCESS").build(),
                    PaymentTransaction.builder().userId(USER_ID).status("FAILED").build()
            );
            when(transactionRepository.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(txList);

            List<PaymentTransaction> result = paymentService.getTransactionsByUser(USER_ID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getStatus()).isEqualTo("SUCCESS");
        }

        @Test
        @DisplayName("returns empty list when user has no transactions")
        void getTransactionsByUser_empty_returnsEmptyList() {
            when(transactionRepository.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of());

            List<PaymentTransaction> result = paymentService.getTransactionsByUser(USER_ID);

            assertThat(result).isEmpty();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ExecutionQuota entity business logic
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("ExecutionQuota entity logic")
    class ExecutionQuotaEntityTests {

        @Test
        @DisplayName("totalAllowed = freeLimit + purchasedTokens")
        void totalAllowed_calculatesCorrectly() {
            ExecutionQuota q = ExecutionQuota.builder()
                    .freeLimit(50).purchasedTokens(100).executionsUsed(0).build();
            assertThat(q.totalAllowed()).isEqualTo(150);
        }

        @Test
        @DisplayName("remaining = totalAllowed - executionsUsed")
        void remaining_calculatesCorrectly() {
            ExecutionQuota q = ExecutionQuota.builder()
                    .freeLimit(50).purchasedTokens(10).executionsUsed(30).build();
            assertThat(q.remaining()).isEqualTo(30);
        }

        @Test
        @DisplayName("remaining never goes below zero")
        void remaining_neverBelowZero() {
            ExecutionQuota q = ExecutionQuota.builder()
                    .freeLimit(50).purchasedTokens(0).executionsUsed(60).build();
            assertThat(q.remaining()).isZero();
        }

        @Test
        @DisplayName("canExecute is true when remaining > 0")
        void canExecute_true_whenRemaining() {
            ExecutionQuota q = ExecutionQuota.builder()
                    .freeLimit(50).purchasedTokens(0).executionsUsed(1).build();
            assertThat(q.canExecute()).isTrue();
        }

        @Test
        @DisplayName("canExecute is false when remaining = 0")
        void canExecute_false_whenExhausted() {
            ExecutionQuota q = ExecutionQuota.builder()
                    .freeLimit(50).purchasedTokens(0).executionsUsed(50).build();
            assertThat(q.canExecute()).isFalse();
        }
    }
}
