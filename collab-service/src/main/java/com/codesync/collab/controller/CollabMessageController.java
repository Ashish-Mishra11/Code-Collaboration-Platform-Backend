package com.codesync.collab.controller;

import com.codesync.collab.service.FileLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@Slf4j
@RequiredArgsConstructor
public class CollabMessageController {

    private final SimpMessagingTemplate messagingTemplate;
    private final FileLockService fileLockService;

    /**
     * FIXED: Receives code edits and broadcasts to all participants in the session.
     * Only the user holding the lock can broadcast edits.
     */
    @MessageMapping("/session/{sessionId}/edit")
    public void processEdit(
            @DestinationVariable String sessionId,
            @Payload Map<String, Object> payload) {

        Object userIdObj = payload.get("userId");
        if (userIdObj == null) {
            log.warn("Edit message missing userId for session {}", sessionId);
            return;
        }
        Integer userId = ((Number) userIdObj).intValue();

        boolean hasLock = fileLockService.acquireLock(sessionId, userId);
        if (hasLock) {
            // Broadcast the full code content to all subscribers of this session
            messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/edit", (Object) payload);
            log.debug("Broadcast edit for session {} from user {}", sessionId, userId);
        } else {
            log.debug("Edit rejected for session {} - user {} does not hold lock", sessionId, userId);
        }
    }

    /**
     * FIXED: Cursor movement - broadcast to all participants.
     */
    @MessageMapping("/session/{sessionId}/cursor")
    public void processCursor(
            @DestinationVariable String sessionId,
            @Payload Map<String, Object> payload) {
        messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/cursor", (Object) payload);
    }

    /**
     * FIXED: Lock request - acquires lock if free and broadcasts lock state.
     */
    @MessageMapping("/session/{sessionId}/request-lock")
    public void requestLock(
            @DestinationVariable String sessionId,
            @Payload Map<String, Object> payload) {
        Object userIdObj = payload.get("userId");
        if (userIdObj == null) return;
        Integer userId = ((Number) userIdObj).intValue();
        boolean acquired = fileLockService.acquireLock(sessionId, userId);
        log.info("Lock request for session {} by user {} - acquired: {}", sessionId, userId, acquired);
    }

    /**
     * FIXED: Lock release - releases lock and broadcasts cleared lock state.
     */
    @MessageMapping("/session/{sessionId}/release-lock")
    public void releaseLock(
            @DestinationVariable String sessionId,
            @Payload Map<String, Object> payload) {
        Object userIdObj = payload.get("userId");
        if (userIdObj == null) return;
        Integer userId = ((Number) userIdObj).intValue();
        fileLockService.releaseLock(sessionId, userId);
        log.info("Lock released for session {} by user {}", sessionId, userId);
    }

    /**
     * Ping/heartbeat - keeps connection alive.
     */
    @MessageMapping("/session/{sessionId}/ping")
    public void ping(
            @DestinationVariable String sessionId,
            @Payload Map<String, Object> payload) {
        messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/pong",
                (Object) Map.of("type", "PONG", "sessionId", sessionId));
    }
}
