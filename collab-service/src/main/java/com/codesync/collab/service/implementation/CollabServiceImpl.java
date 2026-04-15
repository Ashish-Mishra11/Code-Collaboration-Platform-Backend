package com.codesync.collab.service.implementation;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import javax.print.event.PrintJobAttributeEvent;

import org.modelmapper.ModelMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.support.SessionStatus;

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

@Service
@AllArgsConstructor
public class CollabServiceImpl implements CollabService {
	
	private final CollabRepository collabRepository;
    private final UserClient userClient;
    private ModelMapper modelMapper;
    private final SimpMessagingTemplate messagingTemplate;// For WebSocket broadcasting
    private final ParticipantRepository participantRepository;
    
    private static final List<String> AVAILABLE_COLORS = Arrays.asList(
            "#FF5733", "#33FF57", "#3357FF", "#FF33A1", "#A133FF",
            "#33FFF5", "#FFBD33", "#8A2BE2", "#FF8C00", "#00CED1"
    );
    
    
	@Override
	public CreateCollabSessionResponse createSession(CreateCollabSessionRequest request) {
		// TODO Auto-generated method stub
	  Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

	      if (authentication == null || !authentication.isAuthenticated()) {
	            throw new RuntimeException("User is not authenticated. Please login first.");
	      }

	    String username = authentication.getName();
	    System.out.println("inside service collab");
		Long userId = userClient.getUserIdByUsername(username);
        System.out.println("inside collab55555 ");
		// Step B: Create the CollabSession entity
        CollabSession session = new CollabSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setProjectId(request.getProjectId());
        session.setFileId(request.getFileId());
        session.setOwnerId(userId.intValue());                    // from JWT / SecurityContext
        session.setLanguage(request.getLanguage());
        session.setCreatedAt(LocalDateTime.now());
        session.setStatus("ACTIVE");
        session.setMaxParticipants(request.getMaxParticipants() != null ? 
                                   request.getMaxParticipants() : 10);
        session.setIsPasswordProtected(request.getIsPasswordProtected());
        session.setSessionPassword(request.getIsPasswordProtected() ? 
                                   request.getSessionPassword() : null);

        // Step C: Save to local database (only CollabSession table is affected here)
        CollabSession savedSession = collabRepository.save(session);
        CreateCollabSessionResponse map = modelMapper.map(savedSession, CreateCollabSessionResponse.class);
		System.out.println("insdie collab service impl");
        return map;
	}

	@Override
	public Optional<CreateCollabSessionResponse> getSessionById(String sessionId) {
		// TODO Auto-generated method stub

		Optional<CreateCollabSessionResponse> map = collabRepository.findById(sessionId)
        .map(session -> modelMapper.map(session, CreateCollabSessionResponse.class));
		return map;
	}

	@Override
	public List<CreateCollabSessionResponse> getSessionsByProject(Integer projectId) {
		// TODO Auto-generated method stub
//		List<CollabSession> byProjectId = collabRepository.findByProjectId(projectId);
		List<CreateCollabSessionResponse> list = collabRepository.findByProjectId(projectId).stream().map(session->modelMapper.map(session, CreateCollabSessionResponse.class)).toList();
		return list;
	}



	@Override
	@Transactional
	public void leaveSession(String sessionId, Integer userId) {
		// TODO Auto-generated method stub
		// 1. Find the session
	    CollabSession session = collabRepository.findById(sessionId)
	            .orElseThrow(() -> new RuntimeException("Session not found with id: " + sessionId));

	    // 2. Find the participant
	    Participant participant = session.getParticipants().stream()
	            .filter(p -> p.getUserId().equals(userId))
	            .findFirst()
	            .orElseThrow(() -> new RuntimeException("Participant not found in session"));

	    // 3. Mark as left (soft leave)
	    participant.setLeftAt(LocalDateTime.now());

	    // 4. Remove from current active participants list
	    session.getParticipants().remove(participant);

	    // 5. Save changes
	    collabRepository.save(session);
	    
	    //6. delete participant from the participant table
	    participantRepository.delete(participant);

	    // 6. Prepare broadcast message
	    ParticipantLeftMessage leftMessage = ParticipantLeftMessage.builder()
	            .sessionId(sessionId)
	            .userId(userId)
	            .currentParticipantCount(session.getParticipants().size())
	            .build();

	    // 7. Broadcast to all remaining users in the session
	    messagingTemplate.convertAndSend(
	            "/topic/session/" + sessionId + "/participants", 
	            leftMessage
	    );

	    // Optional: You can also broadcast a system message like "User has left the session"
	    System.out.println("User " + userId + " left session " + sessionId);
		
	}

	@Override
	@Transactional
	public void endSession(String sessionId, Integer ownerId) {
		// TODO Auto-generated method stub
		// 1. Find the session
	    CollabSession session = collabRepository.findById(sessionId)
	            .orElseThrow(() -> new RuntimeException("Session not found with id: " + sessionId));

	    // 2. Security Check: Only owner can end the session
	    if (!session.getOwnerId().equals(ownerId)) {
	        throw new RuntimeException("Only the session owner can end the session");
	    }

	    // 3. If already ended, do nothing
	    if ("ENDED".equals(session.getStatus())) {
	        return;
	    }

	    // 4. End the session
	    session.setStatus("ENDED");
	    session.setEndedAt(LocalDateTime.now());

	    // 5. Mark all participants as left
	    for (Participant p : session.getParticipants()) {
	        if (p.getLeftAt() == null) {
	            p.setLeftAt(LocalDateTime.now());
	        }
	    }

	    // 6. Save changes
	    collabRepository.save(session);

	    // 7. Prepare broadcast message
	    SessionEndedMessage endedMessage = SessionEndedMessage.builder()
	            .sessionId(sessionId)
	            .endedByUserId(ownerId)
	            .build();

	    // 8. Broadcast to all participants that session has ended
	    messagingTemplate.convertAndSend(
	            "/topic/session/" + sessionId, 
	            endedMessage
	    );

	    System.out.println("Session " + sessionId + " has been ended by user " + ownerId);
		
	}

	@Override
	@Transactional 
	public List<Participant> getParticipants(String sessionId) {

	    // Find the session
	    CollabSession session = collabRepository.findById(sessionId)
	            .orElseThrow(() -> new RuntimeException("Session not found with id: " + sessionId));

	    // Return only active participants 
	    return session.getParticipants().stream()
	            .filter(participant -> participant.getLeftAt() == null)   // Only active ones
	            .toList();   
	            
	}
	
	@Override
	public void updateCursor(String sessionId, Integer userId, int line, int col) {
		// TODO Auto-generated method stub
        Participant participant = participantRepository.findBySession_SessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));

        participant.setCursorLine(line);
        participant.setCursorCol(col);
        participantRepository.save(participant);

        messagingTemplate.convertAndSend(
                "/topic/session/" + sessionId + "/cursor",
                "User " + userId + " moved cursor to line " + line + ", col " + col
        );
		
	}

//	@Override
//	public void broadcastChange(String sessionId, String change) {
//		// TODO Auto-generated method stub
//		
//	}

	@Override
	@Transactional
	public Participant joinSession(String sessionId, Integer userId, String role, String password) {
		// TODO Auto-generated method stub
		// 1. Find the session
        CollabSession session = collabRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found with id: " + sessionId));

        // 2. Validate session is active
        if (!"ACTIVE".equals(session.getStatus())) {
            throw new RuntimeException("Session has already ended");
        }

        // 3. Check password if protected
        if (Boolean.TRUE.equals(session.getIsPasswordProtected())) {
            if (password == null || !password.equals(session.getSessionPassword())) {
                throw new RuntimeException("Incorrect password");
            }
        }

        // 4. Check if user already joined
        boolean alreadyJoined = session.getParticipants().stream()
                .anyMatch(p -> p.getUserId().equals(userId));
        if (alreadyJoined) {
            throw new RuntimeException("User is already in the session");
        }

        // 5. Check max participants
        if (session.getParticipants().size() >= (session.getMaxParticipants() != null ? session.getMaxParticipants() : 10)) {
            throw new RuntimeException("Session is full");
        }

        // 6. Assign unique color
        String color = assignUniqueColor(session);

        // 7. Create new Participant
        Participant participant = new Participant();
        participant.setSession(session);
        participant.setUserId(userId);
        participant.setRole(role != null ? role.toUpperCase() : "EDITOR");
        participant.setJoinedAt(LocalDateTime.now());
        participant.setCursorLine(0);
        participant.setCursorCol(0);
        participant.setColor(color);

        // 8. Add to session and save
        session.getParticipants().add(participant);
        Participant save = participantRepository.save(participant);
        collabRepository.save(session);

        // 9. Prepare broadcast message
        ParticipantSummary summary = ParticipantSummary.builder()
                .userId(participant.getUserId())
                .role(participant.getRole())
                .color(participant.getColor())
                .cursorLine(participant.getCursorLine())
                .cursorCol(participant.getCursorCol())
                .joinedAt(participant.getJoinedAt())
                .build();

        ParticipantJoinedMessage message = ParticipantJoinedMessage.builder()
                .sessionId(sessionId)
                .participant(summary)
                .currentParticipantCount(session.getParticipants().size())
                .build();

        // 10. Broadcast to all users in this session via WebSocket
        messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/participants", message);

        return participant;
    }

    // Helper method to assign unique color
    private String assignUniqueColor(CollabSession session) {
        Set<String> usedColors = new HashSet<>();
        for (Participant p : session.getParticipants()) {
            usedColors.add(p.getColor());
        }

        for (String color : AVAILABLE_COLORS) {
            if (!usedColors.contains(color)) {
                return color;
            }
        }

        // Fallback random color
        return String.format("#%06X", new Random().nextInt(0xFFFFFF));
    }
		
	
	
	

}
