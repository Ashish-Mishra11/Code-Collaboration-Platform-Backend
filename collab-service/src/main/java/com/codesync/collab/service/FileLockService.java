package com.codesync.collab.service;

import com.codesync.collab.entity.CollabSession;
import com.codesync.collab.repository.CollabRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FileLockService {

    private final CollabRepository sessionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public boolean acquireLock(String sessionId, Integer userId) {
        Optional<CollabSession> optSession = sessionRepository.findById(sessionId);
        if (optSession.isEmpty()) return false;

        CollabSession session = optSession.get();
        Integer currentOwner = session.getActiveEditorUserId();

        // If the user already has the lock, do nothing and return true.
        // This prevents redundant DB saves and STOMP broadcasts on every keystroke.
        if (userId.equals(currentOwner)) {
            return true;
        }

        // If no one has the lock, grant it to the user.
        if (currentOwner == null) {
            session.setActiveEditorUserId(userId);
            sessionRepository.save(session);
            broadcastLockChange(sessionId, userId);
            return true;
        }

        // Someone else has the lock. Broadcast the actual current owner to update the requester.
        broadcastLockChange(sessionId, currentOwner);
        return false;
    }

    @Transactional
    public void releaseLock(String sessionId, Integer userId) {
        Optional<CollabSession> optSession = sessionRepository.findById(sessionId);
        if (optSession.isPresent()) {
            CollabSession session = optSession.get();
            // Only release if the user calling this actually holds the lock
            if (session.getActiveEditorUserId() != null && session.getActiveEditorUserId().equals(userId)) {
                session.setActiveEditorUserId(null);
                sessionRepository.save(session);
                broadcastLockChange(sessionId, null);
            }
        }
    }

    private void broadcastLockChange(String sessionId, Integer activeUserId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "LOCK_UPDATE");
        payload.put("activeEditorUserId", activeUserId);
        messagingTemplate.convertAndSend("/topic/session/" + sessionId, (Object) payload);
    }
}
