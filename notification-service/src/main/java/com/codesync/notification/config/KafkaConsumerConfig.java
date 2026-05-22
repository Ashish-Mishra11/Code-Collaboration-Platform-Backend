package com.codesync.notification.config;

import com.codesync.notification.event.DeveloperApprovedEvent;
import com.codesync.notification.event.PaymentNotificationEvent;
import com.codesync.notification.event.UserLoginEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka consumer configuration.
 *
 * Creates three typed consumer factories (one per event type) and their
 * matching listener container factories.  All use manual ACK mode so
 * offsets are committed only after the email is actually sent.
 */
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private static final String GROUP_ID = "notification-group";

    // ── Generic consumer properties ───────────────────────────────────────

    private Map<String, Object> baseConsumerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return props;
    }

    // ── UserLoginEvent ────────────────────────────────────────────────────

    @Bean
    public ConsumerFactory<String, UserLoginEvent> userLoginConsumerFactory() {
        JsonDeserializer<UserLoginEvent> deser = new JsonDeserializer<>(UserLoginEvent.class, false);
        deser.addTrustedPackages("*");

        Map<String, Object> props = baseConsumerProps();
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deser);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserLoginEvent> userLoginKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, UserLoginEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(userLoginConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    // ── DeveloperApprovedEvent ────────────────────────────────────────────

    @Bean
    public ConsumerFactory<String, DeveloperApprovedEvent> developerApprovedConsumerFactory() {
        JsonDeserializer<DeveloperApprovedEvent> deser =
                new JsonDeserializer<>(DeveloperApprovedEvent.class, false);
        deser.addTrustedPackages("*");

        Map<String, Object> props = baseConsumerProps();
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deser);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DeveloperApprovedEvent> developerApprovedKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, DeveloperApprovedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(developerApprovedConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    // ── PaymentNotificationEvent ─────────────────────────────────────────

    @Bean
    public ConsumerFactory<String, PaymentNotificationEvent> paymentNotificationConsumerFactory() {
        JsonDeserializer<PaymentNotificationEvent> deser =
                new JsonDeserializer<>(PaymentNotificationEvent.class, false);
        deser.addTrustedPackages("*");

        Map<String, Object> props = baseConsumerProps();
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deser);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentNotificationEvent> paymentNotificationKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, PaymentNotificationEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(paymentNotificationConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }
}
