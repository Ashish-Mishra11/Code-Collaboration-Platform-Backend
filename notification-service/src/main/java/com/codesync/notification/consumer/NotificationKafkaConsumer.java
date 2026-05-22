package com.codesync.notification.consumer;

import com.codesync.notification.config.KafkaTopics;
import com.codesync.notification.event.DeveloperApprovedEvent;
import com.codesync.notification.event.PaymentNotificationEvent;
import com.codesync.notification.event.UserLoginEvent;
import com.codesync.notification.service.NotificationEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that listens to all CodeSync notification topics.
 *
 * Each listener:
 *   1. Receives the typed event payload
 *   2. Delegates to {@link NotificationEmailService} for email dispatch
 *   3. Manually acknowledges the offset on success
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationKafkaConsumer {

    private final NotificationEmailService emailService;

    // ── 1. User Login ────────────────────────────────────────────────────────

    @KafkaListener(
            topics = KafkaTopics.USER_LOGIN,
            groupId = "notification-group",
            containerFactory = "userLoginKafkaListenerContainerFactory"
    )
    public void onUserLogin(
            @Payload UserLoginEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {

        log.info("[Consumer] USER_LOGIN received | user={} email={} partition={} offset={}",
                event.getUserName(), event.getEmail(), partition, offset);

        try {
            emailService.sendUserLoginEmail(event);
            ack.acknowledge();
            log.debug("[Consumer] USER_LOGIN acknowledged | offset={}", offset);
        } catch (Exception ex) {
            log.error("[Consumer] Failed processing USER_LOGIN for {}: {}",
                    event.getEmail(), ex.getMessage(), ex);
            // Do NOT ack — message will be redelivered
        }
    }

    // ── 2. Developer Approved ─────────────────────────────────────────────────

    @KafkaListener(
            topics = KafkaTopics.DEVELOPER_APPROVED,
            groupId = "notification-group",
            containerFactory = "developerApprovedKafkaListenerContainerFactory"
    )
    public void onDeveloperApproved(
            @Payload DeveloperApprovedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {

        log.info("[Consumer] DEVELOPER_APPROVED received | name={} email={} partition={} offset={}",
                event.getName(), event.getEmail(), partition, offset);

        try {
            emailService.sendDeveloperApprovedEmail(event);
            ack.acknowledge();
            log.debug("[Consumer] DEVELOPER_APPROVED acknowledged | offset={}", offset);
        } catch (Exception ex) {
            log.error("[Consumer] Failed processing DEVELOPER_APPROVED for {}: {}",
                    event.getEmail(), ex.getMessage(), ex);
        }
    }

    // ── 3. Payment Notification ──────────────────────────────────────────────

    @KafkaListener(
            topics = KafkaTopics.PAYMENT_NOTIFICATION,
            groupId = "notification-group",
            containerFactory = "paymentNotificationKafkaListenerContainerFactory"
    )
    public void onPaymentNotification(
            @Payload PaymentNotificationEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {

        log.info("[Consumer] PAYMENT_NOTIFICATION received | user={} status={} partition={} offset={}",
                event.getUserName(), event.getStatus(), partition, offset);

        try {
            emailService.sendPaymentNotificationEmail(event);
            ack.acknowledge();
            log.debug("[Consumer] PAYMENT_NOTIFICATION acknowledged | offset={}", offset);
        } catch (Exception ex) {
            log.error("[Consumer] Failed processing PAYMENT_NOTIFICATION for {}: {}",
                    event.getEmail(), ex.getMessage(), ex);
        }
    }
}
