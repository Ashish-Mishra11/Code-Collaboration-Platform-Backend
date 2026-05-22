package com.codesync.collab.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple in-memory STOMP broker for /topic and /queue destinations
        config.enableSimpleBroker("/topic", "/queue");
        // Prefix for messages FROM client TO server @MessageMapping methods
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Primary endpoint with SockJS fallback (used by frontend)
        registry.addEndpoint("/collab")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // Raw WebSocket endpoint (no SockJS) for clients that support native WS
        registry.addEndpoint("/collab-ws")
                .setAllowedOriginPatterns("*");
    }
}
