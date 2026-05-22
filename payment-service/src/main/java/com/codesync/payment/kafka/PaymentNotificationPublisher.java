package com.codesync.payment.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Fire-and-forget Kafka publisher for the Payment Service.
 *
 * Payment failures are logged but never propagate back to the caller —
 * the primary transaction outcome must not depend on Kafka availability.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentNotificationPublisher {

    private static final String TOPIC = "codesync.payment.notification";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publishes a payment notification event (SUCCESS or FAILED).
     *
     * @param event the event built by {@link com.codesync.payment.service.impl.PaymentServiceImpl}
     */
    public void publishPaymentNotification(PaymentNotificationEvent event) {
        String key = "payment-" + (event.getUserId() != null
                ? event.getUserId() : UUID.randomUUID());

        try {
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(TOPIC, key, event);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("[PaymentKafka] Failed to publish payment event user={} status={}: {}",
                            event.getUserId(), event.getStatus(), ex.getMessage());
                } else {
                    log.info("[PaymentKafka] Published payment event user={} status={} partition={} offset={}",
                            event.getUserId(), event.getStatus(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });

        } catch (Exception ex) {
            // Kafka is down — log and move on, never block the payment flow
            log.error("[PaymentKafka] Kafka unavailable — payment event dropped user={}: {}",
                    event.getUserId(), ex.getMessage());
        }
    }
}
