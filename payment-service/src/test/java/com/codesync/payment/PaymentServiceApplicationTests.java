package com.codesync.payment;

import com.codesync.payment.dto.QuotaResponseDto;
import com.codesync.payment.dto.TokenPurchaseRequest;
import com.codesync.payment.entity.ExecutionQuota;
import com.codesync.payment.entity.PaymentTransaction;
import com.codesync.payment.exception.DuplicateTransactionException;
import com.codesync.payment.exception.QuotaExceededException;
import com.codesync.payment.repository.ExecutionQuotaRepository;
import com.codesync.payment.repository.PaymentTransactionRepository;
import com.codesync.payment.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Unit Tests")
class PaymentServiceApplicationTests {

    @Mock private ExecutionQuotaRepository quotaRepository;
    @Mock private PaymentTransactionRepository transactionRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private ExecutionQuota freshQuota;
    private ExecutionQuota exhaustedQuota;

    @BeforeEach
    void setUp() {
        freshQuota = ExecutionQuota.builder()
                .id(1L).userId(1).projectId(10).fileId(100)
                .freeLimit(50).executionsUsed(0).purchasedTokens(0)
                .build();

        exhaustedQuota = ExecutionQuota.builder()
                .id(2L).userId(1).projectId(10).fileId(200)
                .freeLimit(50).executionsUsed(50).purchasedTokens(0)
                .build();
    }

    // ── getQuota ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getQuota: returns existing quota when found")
    void getQuota_existing() {
        when(quotaRepository.findByUserIdAndProjectIdAndFileId(1, 10, 100))
                .thenReturn(Optional.of(freshQuota));

        QuotaResponseDto dto = paymentService.getQuota(1, 10, 100);

        assertThat(dto.getRemaining()).isEqualTo(50);
        assertThat(dto.isCanExecute()).isTrue();
        assertThat(dto.getFreeLimit()).isEqualTo(50);
    }

    @Test
    @DisplayName("getQuota: creates and returns new quota when none exists")
    void getQuota_createsNew() {
        when(quotaRepository.findByUserIdAndProjectIdAndFileId(1, 10, 999))
                .thenReturn(Optional.empty());
        when(quotaRepository.save(any())).thenAnswer(inv -> {
            ExecutionQuota q = inv.getArgument(0);
            q.setId(99L);
            return q;
        });

        QuotaResponseDto dto = paymentService.getQuota(1, 10, 999);

        assertThat(dto.getFreeLimit()).isEqualTo(50);
        assertThat(dto.getExecutionsUsed()).isEqualTo(0);
        assertThat(dto.isCanExecute()).isTrue();
        verify(quotaRepository).save(any(ExecutionQuota.class));
    }

    // ── consumeExecution ──────────────────────────────────────────────────────

    @Test
    @DisplayName("consumeExecution: decrements remaining and saves when quota available")
    void consumeExecution_success() {
        when(quotaRepository.findByUserIdAndProjectIdAndFileId(1, 10, 100))
                .thenReturn(Optional.of(freshQuota));
        when(quotaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        QuotaResponseDto dto = paymentService.consumeExecution(1, 10, 100);

        assertThat(dto.getExecutionsUsed()).isEqualTo(1);
        assertThat(dto.getRemaining()).isEqualTo(49);
        verify(quotaRepository).save(freshQuota);
    }

    @Test
    @DisplayName("consumeExecution: throws QuotaExceededException when no remaining slots")
    void consumeExecution_quotaExceeded() {
        when(quotaRepository.findByUserIdAndProjectIdAndFileId(1, 10, 200))
                .thenReturn(Optional.of(exhaustedQuota));

        assertThatThrownBy(() -> paymentService.consumeExecution(1, 10, 200))
                .isInstanceOf(QuotaExceededException.class)
                .hasMessageContaining("limit reached");
    }

    @Test
    @DisplayName("consumeExecution: allows execution when purchased tokens extend limit")
    void consumeExecution_purchasedTokensHelp() {
        exhaustedQuota.setPurchasedTokens(100); // now 50+100=150 total, 50 used => 100 remaining
        when(quotaRepository.findByUserIdAndProjectIdAndFileId(1, 10, 200))
                .thenReturn(Optional.of(exhaustedQuota));
        when(quotaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        QuotaResponseDto dto = paymentService.consumeExecution(1, 10, 200);

        assertThat(dto.isCanExecute()).isTrue();
        assertThat(dto.getExecutionsUsed()).isEqualTo(51);
    }

    // ── purchaseTokens ────────────────────────────────────────────────────────

    @Test
    @DisplayName("purchaseTokens: credits tokens and creates SUCCESS transaction")
    void purchaseTokens_success() {
        TokenPurchaseRequest req = TokenPurchaseRequest.builder()
                .userId(1).projectId(10).fileId(100)
                .tokensToBuy(100).gatewayReference("pay_abc123")
                .build();

        when(transactionRepository.findByGatewayReference("pay_abc123"))
                .thenReturn(Optional.empty());
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(quotaRepository.findByUserIdAndProjectIdAndFileId(1, 10, 100))
                .thenReturn(Optional.of(freshQuota));
        when(quotaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentTransaction tx = paymentService.purchaseTokens(req);

        assertThat(tx.getStatus()).isEqualTo("SUCCESS");
        assertThat(tx.getTokensPurchased()).isEqualTo(100);
        assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(freshQuota.getPurchasedTokens()).isEqualTo(100);
        verify(transactionRepository, atLeast(2)).save(any()); // PENDING then SUCCESS
    }

    @Test
    @DisplayName("purchaseTokens: throws DuplicateTransactionException for repeated gateway reference")
    void purchaseTokens_duplicateGatewayRef() {
        TokenPurchaseRequest req = TokenPurchaseRequest.builder()
                .userId(1).projectId(10).fileId(100)
                .tokensToBuy(100).gatewayReference("pay_already_done")
                .build();

        PaymentTransaction existing = PaymentTransaction.builder()
                .id(1L).status("SUCCESS").gatewayReference("pay_already_done").build();
        when(transactionRepository.findByGatewayReference("pay_already_done"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> paymentService.purchaseTokens(req))
                .isInstanceOf(DuplicateTransactionException.class)
                .hasMessageContaining("already processed");
    }

    @Test
    @DisplayName("purchaseTokens: no gateway reference is allowed (simulates dev/test mode)")
    void purchaseTokens_noGatewayRef() {
        TokenPurchaseRequest req = TokenPurchaseRequest.builder()
                .userId(1).projectId(10).fileId(100).tokensToBuy(50).build();

        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(quotaRepository.findByUserIdAndProjectIdAndFileId(1, 10, 100))
                .thenReturn(Optional.of(freshQuota));
        when(quotaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentTransaction tx = paymentService.purchaseTokens(req);

        assertThat(tx.getStatus()).isEqualTo("SUCCESS");
        assertThat(freshQuota.getPurchasedTokens()).isEqualTo(50);
    }

    // ── getTransactionsByUser ──────────────────────────────────────────────────

    @Test
    @DisplayName("getTransactionsByUser: returns all transactions ordered by date desc")
    void getTransactionsByUser() {
        PaymentTransaction tx1 = PaymentTransaction.builder().id(1L).userId(1).status("SUCCESS").build();
        PaymentTransaction tx2 = PaymentTransaction.builder().id(2L).userId(1).status("FAILED").build();
        when(transactionRepository.findByUserIdOrderByCreatedAtDesc(1))
                .thenReturn(List.of(tx1, tx2));

        List<PaymentTransaction> result = paymentService.getTransactionsByUser(1);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    // ── ExecutionQuota helpers ─────────────────────────────────────────────────

    @Test
    @DisplayName("ExecutionQuota: totalAllowed returns freeLimit + purchasedTokens")
    void executionQuota_totalAllowed() {
        ExecutionQuota q = ExecutionQuota.builder()
                .freeLimit(50).purchasedTokens(100).executionsUsed(0).build();
        assertThat(q.totalAllowed()).isEqualTo(150);
        assertThat(q.remaining()).isEqualTo(150);
        assertThat(q.canExecute()).isTrue();
    }

    @Test
    @DisplayName("ExecutionQuota: remaining never goes below 0")
    void executionQuota_remainingNonNegative() {
        ExecutionQuota q = ExecutionQuota.builder()
                .freeLimit(50).purchasedTokens(0).executionsUsed(60).build();
        assertThat(q.remaining()).isEqualTo(0);
        assertThat(q.canExecute()).isFalse();
    }
}
