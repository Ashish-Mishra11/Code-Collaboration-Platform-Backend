package com.codesync.collab;

import com.codesync.collab.dto.CreateCollabSessionRequest;
import com.codesync.collab.dto.CreateCollabSessionResponse;
import com.codesync.collab.entity.CollabSession;
import com.codesync.collab.entity.Participant;
import com.codesync.collab.exception.ResourceNotFoundException;
import com.codesync.collab.feignclient.UserClient;
import com.codesync.collab.repository.CollabRepository;
import com.codesync.collab.repository.ParticipantRepository;
import com.codesync.collab.service.FileLockService;
import com.codesync.collab.service.implementation.CollabServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CollabService Unit Tests")
class CollabServiceApplicationTests {

    @Mock private CollabRepository collabRepository;
    @Mock private ParticipantRepository participantRepository;
    @Mock private UserClient userClient;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private ModelMapper modelMapper;

    @InjectMocks
    private CollabServiceImpl collabService;

    private CollabSession activeSession;
    private CreateCollabSessionRequest sessionRequest;

    @BeforeEach
    void setUp() {
        activeSession = new CollabSession();
        activeSession.setSessionId("session-abc");
        activeSession.setProjectId(10);
        activeSession.setFileId(100);
        activeSession.setOwnerId(1);
        activeSession.setStatus("ACTIVE");
        activeSession.setMaxParticipants(10);
        activeSession.setIsPasswordProtected(false);
        activeSession.setParticipants(new ArrayList<>());
        activeSession.setCreatedAt(LocalDateTime.now());

        sessionRequest = new CreateCollabSessionRequest();
        sessionRequest.setProjectId(10);
        sessionRequest.setFileId(100);
        sessionRequest.setLanguage("Python");
        sessionRequest.setMaxParticipants(10);
        sessionRequest.setIsPasswordProtected(false);

        // Set up Spring Security context so createSession can read username
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("testuser", null, List.of()));
    }

    // ── createSession ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("createSession: creates session for authenticated user")
    void createSession_success() {
        when(userClient.getUserIdByUsername("testuser")).thenReturn(1L);
        when(collabRepository.save(any())).thenReturn(activeSession);
        CreateCollabSessionResponse mockResponse = new CreateCollabSessionResponse();
        mockResponse.setSessionId("session-abc");
        when(modelMapper.map(any(), eq(CreateCollabSessionResponse.class))).thenReturn(mockResponse);

        CreateCollabSessionResponse response = collabService.createSession(sessionRequest);

        assertThat(response.getSessionId()).isEqualTo("session-abc");
        verify(collabRepository).save(any(CollabSession.class));
    }

    @Test
    @DisplayName("createSession: throws when user not authenticated")
    void createSession_notAuthenticated() {
        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> collabService.createSession(sessionRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not authenticated");
    }

    // ── getSessionById ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getSessionById: returns session when found")
    void getSessionById_found() {
        when(collabRepository.findById("session-abc")).thenReturn(Optional.of(activeSession));
        when(modelMapper.map(any(), eq(CreateCollabSessionResponse.class)))
                .thenReturn(new CreateCollabSessionResponse());

        Optional<CreateCollabSessionResponse> result = collabService.getSessionById("session-abc");
        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("getSessionById: returns empty when session not found")
    void getSessionById_notFound() {
        when(collabRepository.findById("missing")).thenReturn(Optional.empty());
        assertThat(collabService.getSessionById("missing")).isEmpty();
    }

    // ── joinSession ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("joinSession: adds participant to active session")
    void joinSession_success() {
        when(collabRepository.findById("session-abc")).thenReturn(Optional.of(activeSession));
        when(participantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(collabRepository.save(any())).thenReturn(activeSession);

        Participant p = collabService.joinSession("session-abc", 2, "EDITOR", null);

        assertThat(p.getUserId()).isEqualTo(2);
        assertThat(p.getRole()).isEqualTo("EDITOR");
        assertThat(activeSession.getParticipants()).hasSize(1);
        verify(messagingTemplate).convertAndSend(contains("/participants"), any(Object.class));
    }

    @Test
    @DisplayName("joinSession: throws when session not found")
    void joinSession_sessionNotFound() {
        when(collabRepository.findById("bad")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> collabService.joinSession("bad", 1, "EDITOR", null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Session not found");
    }

    @Test
    @DisplayName("joinSession: throws when session has ended")
    void joinSession_sessionEnded() {
        activeSession.setStatus("ENDED");
        when(collabRepository.findById("session-abc")).thenReturn(Optional.of(activeSession));
        assertThatThrownBy(() -> collabService.joinSession("session-abc", 2, "EDITOR", null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ended");
    }

    @Test
    @DisplayName("joinSession: returns existing participant when user already in session")
    void joinSession_alreadyJoined() {
        Participant existing = new Participant();
        existing.setUserId(2);
        activeSession.getParticipants().add(existing);
        when(collabRepository.findById("session-abc")).thenReturn(Optional.of(activeSession));

        Participant result = collabService.joinSession("session-abc", 2, "EDITOR", null);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(2);
    }

    @Test
    @DisplayName("joinSession: rejects with wrong password when session is password-protected")
    void joinSession_wrongPassword() {
        activeSession.setIsPasswordProtected(true);
        activeSession.setSessionPassword("secret");
        when(collabRepository.findById("session-abc")).thenReturn(Optional.of(activeSession));

        assertThatThrownBy(() -> collabService.joinSession("session-abc", 3, "EDITOR", "wrongpass"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Incorrect password");
    }

    // ── leaveSession ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("leaveSession: removes participant and broadcasts")
    void leaveSession_success() {
        Participant p = new Participant();
        p.setUserId(2);
        activeSession.getParticipants().add(p);

        when(collabRepository.findById("session-abc")).thenReturn(Optional.of(activeSession));
        when(collabRepository.save(any())).thenReturn(activeSession);

        collabService.leaveSession("session-abc", 2);

        assertThat(activeSession.getParticipants()).isEmpty();
        verify(participantRepository).delete(p);
        verify(messagingTemplate).convertAndSend(contains("/participants"), any(Object.class));
    }

    // ── endSession ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("endSession: sets status to ENDED and broadcasts")
    void endSession_success() {
        when(collabRepository.findById("session-abc")).thenReturn(Optional.of(activeSession));
        when(collabRepository.save(any())).thenReturn(activeSession);

        collabService.endSession("session-abc", 1); // ownerId=1

        assertThat(activeSession.getStatus()).isEqualTo("ENDED");
        assertThat(activeSession.getEndedAt()).isNotNull();
        verify(messagingTemplate).convertAndSend(eq("/topic/session/session-abc"), any(Object.class));
    }

    @Test
    @DisplayName("endSession: throws when non-owner tries to end session")
    void endSession_notOwner() {
        when(collabRepository.findById("session-abc")).thenReturn(Optional.of(activeSession));
        assertThatThrownBy(() -> collabService.endSession("session-abc", 99)) // not owner
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("owner");
    }

    // ── FileLockService ────────────────────────────────────────────────────────

    @Test
    @DisplayName("FileLockService: acquires lock when session is free")
    void fileLock_acquireFree() {
        activeSession.setActiveEditorUserId(null);
        CollabRepository mockRepo = mock(CollabRepository.class);
        when(mockRepo.findById("session-abc")).thenReturn(Optional.of(activeSession));
        when(mockRepo.save(any())).thenReturn(activeSession);

        FileLockService lockService = new FileLockService(mockRepo, messagingTemplate);
        boolean result = lockService.acquireLock("session-abc", 2);

        assertThat(result).isTrue();
        assertThat(activeSession.getActiveEditorUserId()).isEqualTo(2);
        verify(messagingTemplate).convertAndSend(contains("session-abc"), (Object) any(java.util.Map.class));
    }

    @Test
    @DisplayName("FileLockService: rejects lock when another user holds it")
    void fileLock_acquireWhenHeldByOther() {
        activeSession.setActiveEditorUserId(1); // user 1 holds lock
        CollabRepository mockRepo = mock(CollabRepository.class);
        when(mockRepo.findById("session-abc")).thenReturn(Optional.of(activeSession));

        FileLockService lockService = new FileLockService(mockRepo, messagingTemplate);
        boolean result = lockService.acquireLock("session-abc", 2); // user 2 tries

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("FileLockService: releases lock for holding user")
    void fileLock_releaseLock() {
        activeSession.setActiveEditorUserId(2);
        CollabRepository mockRepo = mock(CollabRepository.class);
        when(mockRepo.findById("session-abc")).thenReturn(Optional.of(activeSession));
        when(mockRepo.save(any())).thenReturn(activeSession);

        FileLockService lockService = new FileLockService(mockRepo, messagingTemplate);
        lockService.releaseLock("session-abc", 2);

        assertThat(activeSession.getActiveEditorUserId()).isNull();
    }
}
