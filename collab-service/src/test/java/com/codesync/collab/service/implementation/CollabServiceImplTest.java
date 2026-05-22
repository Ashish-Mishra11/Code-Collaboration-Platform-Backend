package com.codesync.collab.service.implementation;

import com.codesync.collab.dto.CreateCollabSessionRequest;
import com.codesync.collab.dto.CreateCollabSessionResponse;
import com.codesync.collab.entity.CollabSession;
import com.codesync.collab.entity.Participant;
import com.codesync.collab.exception.ResourceNotFoundException;
import com.codesync.collab.feignclient.UserClient;
import com.codesync.collab.repository.CollabRepository;
import com.codesync.collab.repository.ParticipantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CollabServiceImplTest {

    @InjectMocks
    private CollabServiceImpl collabService;

    @Mock
    private CollabRepository collabRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    private CollabSession session;
    private Participant participant;

    @BeforeEach
    void setUp() {
        session = new CollabSession();
        session.setSessionId("session-123");
        session.setProjectId(1);
        session.setOwnerId(100);
        session.setStatus("ACTIVE");
        session.setMaxParticipants(10);
        session.setParticipants(new ArrayList<>());

        participant = new Participant();
        participant.setParticipantId(1L);
        participant.setUserId(200);
        participant.setSession(session);
        participant.setColor("#FF5733");
    }

    @Test
    void testCreateSession_NotAuthenticated() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(null);

        assertThrows(RuntimeException.class, () -> {
            collabService.createSession(new CreateCollabSessionRequest());
        });
    }

    @Test
    void testCreateSession_Success_WithDetailsHeader() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(authentication.getDetails()).thenReturn("100"); // userId in header

        CreateCollabSessionRequest req = new CreateCollabSessionRequest();
        req.setProjectId(1);
        req.setFileId(2);
        req.setIsPasswordProtected(true);
        req.setSessionPassword("secret");

        when(collabRepository.save(any(CollabSession.class))).thenReturn(session);
        when(modelMapper.map(any(), eq(CreateCollabSessionResponse.class))).thenReturn(new CreateCollabSessionResponse());

        CreateCollabSessionResponse res = collabService.createSession(req);
        
        assertNotNull(res);
        verify(collabRepository).save(any(CollabSession.class));
        verify(userClient, never()).getUserIdByUsername(anyString());
    }

    @Test
    void testCreateSession_Success_FallbackUserClient() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(authentication.getDetails()).thenReturn(null); // Force exception -> fallback
        when(userClient.getUserIdByUsername("testuser")).thenReturn(100L);

        CreateCollabSessionRequest req = new CreateCollabSessionRequest();
        when(collabRepository.save(any(CollabSession.class))).thenReturn(session);
        when(modelMapper.map(any(), eq(CreateCollabSessionResponse.class))).thenReturn(new CreateCollabSessionResponse());

        CreateCollabSessionResponse res = collabService.createSession(req);

        assertNotNull(res);
        verify(userClient).getUserIdByUsername("testuser");
    }

    @Test
    void testGetSessionById_Found() {
        when(collabRepository.findById("session-123")).thenReturn(Optional.of(session));
        when(modelMapper.map(session, CreateCollabSessionResponse.class)).thenReturn(new CreateCollabSessionResponse());

        Optional<CreateCollabSessionResponse> res = collabService.getSessionById("session-123");
        assertTrue(res.isPresent());
    }

    @Test
    void testGetSessionById_NotFound() {
        when(collabRepository.findById("invalid")).thenReturn(Optional.empty());
        Optional<CreateCollabSessionResponse> res = collabService.getSessionById("invalid");
        assertFalse(res.isPresent());
    }

    @Test
    void testGetSessionsByProject() {
        when(collabRepository.findByProjectId(1)).thenReturn(List.of(session));
        when(modelMapper.map(session, CreateCollabSessionResponse.class)).thenReturn(new CreateCollabSessionResponse());

        List<CreateCollabSessionResponse> list = collabService.getSessionsByProject(1);
        assertEquals(1, list.size());
    }

    @Test
    void testJoinSession_Success() {
        when(collabRepository.findById("session-123")).thenReturn(Optional.of(session));
        when(participantRepository.save(any(Participant.class))).thenAnswer(i -> i.getArgument(0));

        Participant p = collabService.joinSession("session-123", 200, "EDITOR", null);

        assertEquals(200, p.getUserId());
        assertEquals("EDITOR", p.getRole());
        assertEquals(1, session.getParticipants().size());
        verify(messagingTemplate).convertAndSend(eq("/topic/session/session-123/participants"), any(Object.class));
    }

    @Test
    void testJoinSession_AlreadyJoined() {
        session.getParticipants().add(participant); // User 200 already present
        when(collabRepository.findById("session-123")).thenReturn(Optional.of(session));

        Participant p = collabService.joinSession("session-123", 200, "EDITOR", null);
        
        assertEquals(participant, p);
        verify(participantRepository, never()).save(any());
    }

    @Test
    void testJoinSession_SessionEnded() {
        session.setStatus("ENDED");
        when(collabRepository.findById("session-123")).thenReturn(Optional.of(session));

        assertThrows(RuntimeException.class, () -> {
            collabService.joinSession("session-123", 200, "EDITOR", null);
        }, "Session has already ended");
    }

    @Test
    void testJoinSession_IncorrectPassword() {
        session.setIsPasswordProtected(true);
        session.setSessionPassword("secret");
        when(collabRepository.findById("session-123")).thenReturn(Optional.of(session));

        assertThrows(RuntimeException.class, () -> {
            collabService.joinSession("session-123", 200, "EDITOR", "wrong");
        }, "Incorrect password");
    }

    @Test
    void testJoinSession_SessionFull() {
        session.setMaxParticipants(1);
        Participant other = new Participant();
        other.setUserId(999);
        session.getParticipants().add(other);
        
        when(collabRepository.findById("session-123")).thenReturn(Optional.of(session));

        assertThrows(RuntimeException.class, () -> {
            collabService.joinSession("session-123", 200, "EDITOR", null);
        }, "Session is full");
    }

    @Test
    void testLeaveSession_Success() {
        session.getParticipants().add(participant);
        when(collabRepository.findById("session-123")).thenReturn(Optional.of(session));

        collabService.leaveSession("session-123", 200);

        assertTrue(session.getParticipants().isEmpty());
        verify(participantRepository).delete(participant);
        verify(collabRepository).save(session);
        verify(messagingTemplate).convertAndSend(eq("/topic/session/session-123/participants"), any(Object.class));
    }

    @Test
    void testLeaveSession_ParticipantNotFound() {
        when(collabRepository.findById("session-123")).thenReturn(Optional.of(session));

        assertThrows(RuntimeException.class, () -> {
            collabService.leaveSession("session-123", 200);
        });
    }

    @Test
    void testEndSession_Success() {
        session.getParticipants().add(participant);
        when(collabRepository.findById("session-123")).thenReturn(Optional.of(session));

        collabService.endSession("session-123", 100);

        assertEquals("ENDED", session.getStatus());
        assertNotNull(session.getEndedAt());
        assertNotNull(participant.getLeftAt());
        verify(collabRepository).save(session);
        verify(messagingTemplate).convertAndSend(eq("/topic/session/session-123"), any(Object.class));
    }

    @Test
    void testEndSession_NotOwner() {
        when(collabRepository.findById("session-123")).thenReturn(Optional.of(session));

        assertThrows(RuntimeException.class, () -> {
            collabService.endSession("session-123", 999);
        });
    }

    @Test
    void testGetParticipants() {
        session.getParticipants().add(participant);
        Participant p2 = new Participant();
        p2.setUserId(201);
        p2.setLeftAt(LocalDateTime.now()); // Should be excluded
        session.getParticipants().add(p2);
        
        when(collabRepository.findById("session-123")).thenReturn(Optional.of(session));

        List<Participant> result = collabService.getParticipants("session-123");
        assertEquals(1, result.size());
        assertEquals(200, result.get(0).getUserId());
    }

    @Test
    void testUpdateCursor_Success() {
        when(participantRepository.findBySession_SessionIdAndUserId("session-123", 200))
                .thenReturn(Optional.of(participant));

        collabService.updateCursor("session-123", 200, 10, 5);

        assertEquals(10, participant.getCursorLine());
        assertEquals(5, participant.getCursorCol());
        verify(participantRepository).save(participant);
        verify(messagingTemplate).convertAndSend(eq("/topic/session/session-123/cursor"), any(Object.class));
    }

    @Test
    void testUpdateCursor_NotFound() {
        when(participantRepository.findBySession_SessionIdAndUserId("session-123", 200))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            collabService.updateCursor("session-123", 200, 10, 5);
        });
    }
}
