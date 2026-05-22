package com.codesync.notification.service;

import com.codesync.notification.event.DeveloperApprovedEvent;
import com.codesync.notification.event.PaymentNotificationEvent;
import com.codesync.notification.event.UserLoginEvent;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link NotificationEmailService}.
 *
 * We mock {@link JavaMailSender} so no real SMTP connection is needed.
 */
@ExtendWith(MockitoExtension.class)
class NotificationEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private NotificationEmailService emailService;

    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() throws Exception {
        // Inject @Value field
        ReflectionTestUtils.setField(emailService, "fromAddress", "test@codesync.com");

        // Create a real MimeMessage to avoid MimeMessageHelper mock issues
        jakarta.mail.Session session = jakarta.mail.Session.getInstance(new java.util.Properties());
        mimeMessage = new MimeMessage(session);

        // mailSender.createMimeMessage() returns our real message
        lenient().when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    // ── 1. Login email ────────────────────────────────────────────────────

    @Test
    @DisplayName("sendUserLoginEmail: should call mailSender.send() once")
    void sendUserLoginEmail_shouldSendEmail() {
        UserLoginEvent event = UserLoginEvent.builder()
                .userId(1)
                .userName("ashish")
                .email("ashish@example.com")
                .fullName("Ashish Mishra")
                .loginTime(LocalDateTime.now())
                .ipAddress("192.168.1.10")
                .build();

        emailService.sendUserLoginEmail(event);

        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    @DisplayName("sendUserLoginEmail: should NOT throw even when fullName is null")
    void sendUserLoginEmail_nullFullName_noException() {
        UserLoginEvent event = UserLoginEvent.builder()
                .userId(2)
                .userName("dev_user")
                .email("dev@example.com")
                .fullName(null)
                .loginTime(LocalDateTime.now())
                .build();

        emailService.sendUserLoginEmail(event);

        verify(mailSender, atLeastOnce()).send(mimeMessage);
    }

    // ── 2. Developer approved email ───────────────────────────────────────

    @Test
    @DisplayName("sendDeveloperApprovedEmail: should call mailSender.send() once")
    void sendDeveloperApprovedEmail_shouldSendEmail() {
        DeveloperApprovedEvent event = DeveloperApprovedEvent.builder()
                .name("Rahul Singh")
                .email("rahul@techcorp.com")
                .companyName("TechCorp Pvt. Ltd.")
                .registrationLink("http://localhost:3000/register-developer?email=rahul@techcorp.com")
                .approvedAt(LocalDateTime.now())
                .build();

        emailService.sendDeveloperApprovedEmail(event);

        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    @DisplayName("sendDeveloperApprovedEmail: companyName null should not throw")
    void sendDeveloperApprovedEmail_nullCompany_noException() {
        DeveloperApprovedEvent event = DeveloperApprovedEvent.builder()
                .name("Anonymous Dev")
                .email("anon@example.com")
                .companyName(null)
                .registrationLink("http://localhost:3000/register-developer?email=anon@example.com")
                .build();

        emailService.sendDeveloperApprovedEmail(event);

        verify(mailSender, atLeastOnce()).send(mimeMessage);
    }

    // ── 3. Payment notification emails ───────────────────────────────────

    @Test
    @DisplayName("sendPaymentNotificationEmail: SUCCESS status should send success email")
    void sendPaymentEmail_success() {
        PaymentNotificationEvent event = PaymentNotificationEvent.builder()
                .userId(10)
                .email("dev@example.com")
                .userName("dev_ashish")
                .tokensPurchased(100)
                .amount(new BigDecimal("49.00"))
                .gatewayReference("order_xyz123")
                .status("SUCCESS")
                .remarks("Credited 100 tokens. New balance: 150")
                .transactionTime(LocalDateTime.now())
                .build();

        emailService.sendPaymentNotificationEmail(event);

        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    @DisplayName("sendPaymentNotificationEmail: FAILED status should send failure email")
    void sendPaymentEmail_failure() {
        PaymentNotificationEvent event = PaymentNotificationEvent.builder()
                .userId(10)
                .email("dev@example.com")
                .userName("dev_ashish")
                .tokensPurchased(200)
                .amount(new BigDecimal("98.00"))
                .gatewayReference("order_failed_456")
                .status("FAILED")
                .remarks("Error: Gateway timeout")
                .transactionTime(LocalDateTime.now())
                .build();

        emailService.sendPaymentNotificationEmail(event);

        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    @DisplayName("sendPaymentNotificationEmail: null email should not propagate exception")
    void sendPaymentEmail_nullEmail_handledGracefully() {
        PaymentNotificationEvent event = PaymentNotificationEvent.builder()
                .userId(99)
                .email(null)    // intentionally null — service must log and swallow
                .userName("ghost")
                .tokensPurchased(50)
                .amount(BigDecimal.ZERO)
                .status("SUCCESS")
                .build();

        // Should not throw — the service logs the error internally
        emailService.sendPaymentNotificationEmail(event);
    }
}
