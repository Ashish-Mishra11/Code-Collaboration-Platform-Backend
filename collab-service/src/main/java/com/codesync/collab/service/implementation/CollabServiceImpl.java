package com.codesync.collab.service.implementation;

import com.codesync.collab.dto.CreateCollabSessionRequest;
import com.codesync.collab.dto.CreateCollabSessionResponse;
import com.codesync.collab.dto.ParticipantJoinedMessage;
import com.codesync.collab.dto.ParticipantLeftMessage;
import com.codesync.collab.dto.ParticipantSummary;
import com.codesync.collab.dto.SessionEndedMessage;
import com.codesync.collab.entity.CollabSession;
import com.codesync.collab.entity.Participant;
import com.codesync.collab.exception.ResourceNotFoundException;
import com.codesync.collab.feignclient.UserClient;
import com.codesync.collab.repository.CollabRepository;
import com.codesync.collab.repository.ParticipantRepository;
import com.codesync.collab.service.CollabService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class CollabServiceImpl implements CollabService {

    private final CollabRepository collabRepository;
    private final UserClient userClient;
    private final ModelMapper modelMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final ParticipantRepository participantRepository;

    private static final List<String> AVAILABLE_COLORS = Arrays.asList(
            "#FF5733", "#33FF57", "#3357FF", "#FF33A1", "#A133FF",
            "#33FFF5", "#FFBD33", "#8A2BE2", "#FF8C00", "#00CED1"
    );

    @Override
    public CreateCollabSessionResponse createSession(CreateCollabSessionRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User is not authenticated. Please login first.");
        }
        String username = authentication.getName();
        Integer userId;
        try {
            // Get userId from the detail set by UserContextFilter (via X-User-Id header)
            userId = Integer.parseInt((String) authentication.getDetails());
        } catch (Exception e) {
            log.warn("Could not get userId from header, falling back to auth-service: {}", e.getMessage());
            userId = userClient.getUserIdByUsername(username).intValue();
        }

        CollabSession session = new CollabSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setProjectId(request.getProjectId());
        session.setFileId(request.getFileId());
        session.setOwnerId(userId);
        session.setLanguage(request.getLanguage());
        session.setCreatedAt(LocalDateTime.now());
        session.setStatus("ACTIVE");
        session.setMaxParticipants(request.getMaxParticipants() != null ? request.getMaxParticipants() : 10);
        session.setIsPasswordProtected(request.getIsPasswordProtected());
        session.setSessionPassword(Boolean.TRUE.equals(request.getIsPasswordProtected())
                ? request.getSessionPassword() : null);

        CollabSession saved = collabRepository.save(session);
        log.info("Created collab session {} for project {}", saved.getSessionId(), saved.getProjectId());
        return modelMapper.map(saved, CreateCollabSessionResponse.class);
    }

    @Override
    public Optional<CreateCollabSessionResponse> getSessionById(String sessionId) {
        return collabRepository.findById(sessionId)
                .map(session -> modelMapper.map(session, CreateCollabSessionResponse.class));
    }

    @Override
    public List<CreateCollabSessionResponse> getSessionsByProject(Integer projectId) {
        return collabRepository.findByProjectId(projectId).stream()
                .map(session -> modelMapper.map(session, CreateCollabSessionResponse.class))
                .toList();
    }

    @Override
    @Transactional
    public Participant joinSession(String sessionId, Integer userId, String role, String password) {
        CollabSession session = collabRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found with id: " + sessionId));

        if (!"ACTIVE".equals(session.getStatus())) {
            throw new RuntimeException("Session has already ended");
        }
        if (Boolean.TRUE.equals(session.getIsPasswordProtected())) {
            if (password == null || !password.equals(session.getSessionPassword())) {
                throw new RuntimeException("Incorrect password");
            }
        }
        // If user already joined, simply return the existing participant instead of throwing
        Optional<Participant> existingParticipant = session.getParticipants().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst();
        
        if (existingParticipant.isPresent()) {
            return existingParticipant.get();
        }
        int maxP = session.getMaxParticipants() != null ? session.getMaxParticipants() : 10;
        if (session.getParticipants().size() >= maxP) {
            throw new RuntimeException("Session is full");
        }

        String color = assignUniqueColor(session);
        Participant participant = new Participant();
        participant.setSession(session);
        participant.setUserId(userId);
        participant.setRole(role != null ? role.toUpperCase() : "EDITOR");
        participant.setJoinedAt(LocalDateTime.now());
        participant.setCursorLine(0);
        participant.setCursorCol(0);
        participant.setColor(color);

        session.getParticipants().add(participant);
        Participant saved = participantRepository.save(participant);
        collabRepository.save(session);

        ParticipantSummary summary = ParticipantSummary.builder()
                .userId(participant.getUserId()).role(participant.getRole())
                .color(participant.getColor()).cursorLine(0).cursorCol(0)
                .joinedAt(participant.getJoinedAt()).build();

        ParticipantJoinedMessage message = ParticipantJoinedMessage.builder()
                .sessionId(sessionId).participant(summary)
                .currentParticipantCount(session.getParticipants().size()).build();

        messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/participants", (Object) message);
        log.info("User {} joined session {}", userId, sessionId);
        return saved;
    }

    @Override
    @Transactional
    public void leaveSession(String sessionId, Integer userId) {
        CollabSession session = collabRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found with id: " + sessionId));

        // Find all matching participants in case of duplicates caused by race conditions
        List<Participant> userParticipants = session.getParticipants().stream()
                .filter(p -> p.getUserId().equals(userId))
                .toList();

        if (userParticipants.isEmpty()) {
            throw new RuntimeException("Participant not found in session");
        }

        // Remove all to clean up any duplicates
        for (Participant participant : userParticipants) {
            participant.setLeftAt(LocalDateTime.now());
            session.getParticipants().remove(participant);
            participantRepository.delete(participant);
        }
        collabRepository.save(session);

        ParticipantLeftMessage leftMessage = ParticipantLeftMessage.builder()
                .sessionId(sessionId).userId(userId)
                .currentParticipantCount(session.getParticipants().size()).build();

        messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/participants", (Object) leftMessage);
        log.info("User {} left session {}", userId, sessionId);
    }

    @Override
    @Transactional
    public void endSession(String sessionId, Integer ownerId) {
        CollabSession session = collabRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found with id: " + sessionId));

        if (!session.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("Only the session owner can end the session");
        }
        if ("ENDED".equals(session.getStatus())) return;

        session.setStatus("ENDED");
        session.setEndedAt(LocalDateTime.now());
        session.getParticipants().forEach(p -> {
            if (p.getLeftAt() == null) p.setLeftAt(LocalDateTime.now());
        });
        collabRepository.save(session);

        SessionEndedMessage endedMessage = SessionEndedMessage.builder()
                .sessionId(sessionId).endedByUserId(ownerId).build();
        messagingTemplate.convertAndSend("/topic/session/" + sessionId, (Object) endedMessage);
        log.info("Session {} ended by owner {}", sessionId, ownerId);
    }

    @Override
    @Transactional
    public List<Participant> getParticipants(String sessionId) {
        CollabSession session = collabRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found with id: " + sessionId));
        
        // Use a Map to guarantee unique users even if DB has duplicates
        java.util.Map<Integer, Participant> distinctParticipants = new java.util.HashMap<>();
        for (Participant p : session.getParticipants()) {
            if (p.getLeftAt() == null) {
                distinctParticipants.put(p.getUserId(), p);
            }
        }
        return new java.util.ArrayList<>(distinctParticipants.values());
    }

    @Override
    public void updateCursor(String sessionId, Integer userId, int line, int col) {
        Participant participant = participantRepository.findBySession_SessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));
        participant.setCursorLine(line);
        participant.setCursorCol(col);
        participantRepository.save(participant);
        messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/cursor",
                (Object) ("User " + userId + " moved cursor to line " + line + ", col " + col));
    }

    private String assignUniqueColor(CollabSession session) {
        Set<String> usedColors = new HashSet<>();
        session.getParticipants().forEach(p -> usedColors.add(p.getColor()));
        for (String color : AVAILABLE_COLORS) {
            if (!usedColors.contains(color)) return color;
        }
        return String.format("#%06X", new Random().nextInt(0xFFFFFF));
    }
}
