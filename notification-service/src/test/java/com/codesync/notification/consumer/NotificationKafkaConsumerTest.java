package com.codesync.notification.consumer;

import com.codesync.notification.event.DeveloperApprovedEvent;
import com.codesync.notification.event.PaymentNotificationEvent;
import com.codesync.notification.event.UserLoginEvent;
import com.codesync.notification.service.NotificationEmailService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link NotificationKafkaConsumer}.
 * Verifies delegation to {@link NotificationEmailService} and correct
 * acknowledgement behaviour without requiring a real Kafka broker.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationKafkaConsumer Unit Tests")
class NotificationKafkaConsumerTest {

    @Mock private NotificationEmailService emailService;
    @Mock private Acknowledgment ack;

    @InjectMocks
    private NotificationKafkaConsumer consumer;

    // ── onUserLogin ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("onUserLogin()")
    class OnUserLoginTests {

        @Test
        @DisplayName("delegates to emailService and acknowledges on success")
        void onUserLogin_success_delegatesAndAcknowledges() {
            UserLoginEvent event = UserLoginEvent.builder()
                    .userId(1).userName("ashish").email("ashish@example.com")
                    .fullName("Ashish Mishra").loginTime(LocalDateTime.now()).build();

            consumer.onUserLogin(event, 0, 0L, ack);

            verify(emailService).sendUserLoginEmail(event);
            verify(ack).acknowledge();
        }

        @Test
        @DisplayName("does NOT acknowledge when emailService throws")
        void onUserLogin_emailFails_doesNotAcknowledge() {
            UserLoginEvent event = UserLoginEvent.builder()
                    .userId(2).userName("user2").email("u@x.com")
                    .loginTime(LocalDateTime.now()).build();

            doThrow(new RuntimeException("SMTP error")).when(emailService).sendUserLoginEmail(event);

            consumer.onUserLogin(event, 0, 1L, ack);

            verify(emailService).sendUserLoginEmail(event);
            verify(ack, never()).acknowledge();
        }

        @Test
        @DisplayName("handles event with null IP address without throwing")
        void onUserLogin_nullIp_noException() {
            UserLoginEvent event = UserLoginEvent.builder()
                    .userId(3).userName("u3").email("u3@x.com")
                    .loginTime(LocalDateTime.now()).ipAddress(null).build();

            consumer.onUserLogin(event, 0, 2L, ack);

            verify(ack).acknowledge();
        }
    }

    // ── onDeveloperApproved ───────────────────────────────────────────────────

    @Nested
    @DisplayName("onDeveloperApproved()")
    class OnDeveloperApprovedTests {

        @Test
        @DisplayName("delegates to emailService and acknowledges on success")
        void onDeveloperApproved_success_delegatesAndAcknowledges() {
            DeveloperApprovedEvent event = DeveloperApprovedEvent.builder()
                    .name("Rahul Singh").email("rahul@techcorp.com")
                    .companyName("TechCorp").registrationLink("http://reg.link")
                    .approvedAt(LocalDateTime.now()).build();

            consumer.onDeveloperApproved(event, 0, 5L, ack);

            verify(emailService).sendDeveloperApprovedEmail(event);
            verify(ack).acknowledge();
        }

        @Test
        @DisplayName("does NOT acknowledge when emailService throws")
        void onDeveloperApproved_emailFails_doesNotAcknowledge() {
            DeveloperApprovedEvent event = DeveloperApprovedEvent.builder()
                    .name("Dev").email("dev@x.com")
                    .registrationLink("http://link").build();

            doThrow(new RuntimeException("Mail error")).when(emailService).sendDeveloperApprovedEmail(event);

            consumer.onDeveloperApproved(event, 0, 6L, ack);

            verify(ack, never()).acknowledge();
        }

        @Test
        @DisplayName("handles null companyName without throwing")
        void onDeveloperApproved_nullCompany_noException() {
            DeveloperApprovedEvent event = DeveloperApprovedEvent.builder()
                    .name("Anon").email("anon@x.com").companyName(null)
                    .registrationLink("http://link").build();

            consumer.onDeveloperApproved(event, 0, 7L, ack);

            verify(ack).acknowledge();
        }
    }

    // ── onPaymentNotification ─────────────────────────────────────────────────

    @Nested
    @DisplayName("onPaymentNotification()")
    class OnPaymentNotificationTests {

        @Test
        @DisplayName("delegates to emailService and acknowledges on SUCCESS event")
        void onPaymentNotification_success_delegatesAndAcknowledges() {
            PaymentNotificationEvent event = PaymentNotificationEvent.builder()
                    .userId(10).email("dev@example.com").userName("dev_ashish")
                    .tokensPurchased(100).amount(new BigDecimal("500.00"))
                    .gatewayReference("order_xyz").status("SUCCESS")
                    .transactionTime(LocalDateTime.now()).build();

            consumer.onPaymentNotification(event, 0, 10L, ack);

            verify(emailService).sendPaymentNotificationEmail(event);
            verify(ack).acknowledge();
        }

        @Test
        @DisplayName("delegates to emailService and acknowledges on FAILED event")
        void onPaymentNotification_failed_delegatesAndAcknowledges() {
            PaymentNotificationEvent event = PaymentNotificationEvent.builder()
                    .userId(11).email("dev@example.com").userName("dev_fail")
                    .tokensPurchased(0).amount(BigDecimal.ZERO)
                    .gatewayReference("order_fail").status("FAILED")
                    .transactionTime(LocalDateTime.now()).build();

            consumer.onPaymentNotification(event, 0, 11L, ack);

            verify(emailService).sendPaymentNotificationEmail(event);
            verify(ack).acknowledge();
        }

        @Test
        @DisplayName("does NOT acknowledge when emailService throws")
        void onPaymentNotification_emailFails_doesNotAcknowledge() {
            PaymentNotificationEvent event = PaymentNotificationEvent.builder()
                    .userId(12).email("fail@example.com").userName("fail_user")
                    .status("SUCCESS").transactionTime(LocalDateTime.now()).build();

            doThrow(new RuntimeException("SMTP timeout"))
                    .when(emailService).sendPaymentNotificationEmail(event);

            consumer.onPaymentNotification(event, 0, 12L, ack);

            verify(ack, never()).acknowledge();
        }

        @Test
        @DisplayName("handles null email without propagating exception")
        void onPaymentNotification_nullEmail_handledGracefully() {
            PaymentNotificationEvent event = PaymentNotificationEvent.builder()
                    .userId(99).email(null).userName("ghost")
                    .tokensPurchased(0).amount(BigDecimal.ZERO).status("SUCCESS")
                    .transactionTime(LocalDateTime.now()).build();

            // emailService should still be called (it handles null gracefully internally)
            consumer.onPaymentNotification(event, 0, 13L, ack);

            verify(emailService).sendPaymentNotificationEmail(event);
        }
    }
}
