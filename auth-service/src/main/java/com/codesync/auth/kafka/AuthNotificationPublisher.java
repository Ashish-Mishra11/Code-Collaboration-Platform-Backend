package com.codesync.auth.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Thin wrapper around {@link KafkaTemplate} for the Auth Service.
 *
 * All publishing is fire-and-forget with async callback logging.
 * A Kafka unavailability will NOT fail the primary auth operations —
 * the exception is caught and logged.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthNotificationPublisher {

    private static final String TOPIC_USER_LOGIN      = "codesync.user.login";
    private static final String TOPIC_DEV_APPROVED    = "codesync.developer.approved";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publishes a login event after a successful authentication.
     */
    public void publishUserLogin(UserLoginEvent event) {
        String key = "login-" + (event.getUserId() != null ? event.getUserId() : UUID.randomUUID());
        sendAsync(TOPIC_USER_LOGIN, key, event);
    }

    /**
     * Publishes a developer-approved event when admin clicks "Approve".
     */
    public void publishDeveloperApproved(DeveloperApprovedEvent event) {
        String key = "dev-approved-" + event.getEmail();
        sendAsync(TOPIC_DEV_APPROVED, key, event);
    }

    // ── internal ──────────────────────────────────────────────────────────

    private void sendAsync(String topic, String key, Object payload) {
        try {
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(topic, key, payload);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("[AuthKafka] Failed to publish to topic={} key={}: {}",
                            topic, key, ex.getMessage());
                } else {
                    log.info("[AuthKafka] Published topic={} key={} partition={} offset={}",
                            topic, key,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });
        } catch (Exception ex) {
            // Kafka is down — log and continue; do NOT block auth flow
            log.error("[AuthKafka] Kafka unavailable — event dropped topic={} key={}: {}",
                    topic, key, ex.getMessage());
        }
    }
}
