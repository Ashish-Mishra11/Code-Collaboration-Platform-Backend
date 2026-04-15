package com.codesync.collab.controller;

import com.codesync.collab.dto.CreateCollabSessionRequest;
import com.codesync.collab.dto.CreateCollabSessionResponse;
import com.codesync.collab.dto.JoinSessionRequest;
import com.codesync.collab.dto.ParticipantSummary;
import com.codesync.collab.dto.UpdateCursorRequest;
import com.codesync.collab.entity.Participant;
import com.codesync.collab.exception.ResourceNotFoundException;
import com.codesync.collab.feignclient.UserClient;
import com.codesync.collab.service.CollabService;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class CollabResource {

    private final CollabService collabService;
    private final UserClient userClient;
    

    //create Session
    @PostMapping
    public ResponseEntity<CreateCollabSessionResponse> createSession(
            @RequestBody CreateCollabSessionRequest request) {

    	System.out.println("Inside create session");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User is not authenticated. Please login first.");
        }

        // Optional: You can add more validation here if needed
        if (request.getProjectId() == null || request.getFileId() == null) {
            throw new RuntimeException("Project ID and File ID are required");
        }

        CreateCollabSessionResponse response = collabService.createSession(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
//    Get sessionById 
    @GetMapping("/{sessionId}")
    public ResponseEntity<CreateCollabSessionResponse> getSessionById(@PathVariable String sessionId) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User is not authenticated. Please login first.");
        }

        CreateCollabSessionResponse orElseThrow = collabService.getSessionById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + sessionId));

        return ResponseEntity.ok(orElseThrow);
    }

//    get Session by Project
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<CreateCollabSessionResponse>> getSessionsByProject(
            @PathVariable Integer projectId) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User is not authenticated. Please login first.");
        }

        List<CreateCollabSessionResponse> sessions = collabService.getSessionsByProject(projectId);
        return ResponseEntity.ok(sessions);
    }
    

    //join the session
    @PostMapping("/{sessionId}/join")
    public ResponseEntity<ParticipantSummary> joinSession(
            @PathVariable String sessionId,
            @RequestBody(required = false) JoinSessionRequest request) {   

        // Get userId from JWT / Security (for now we assume you pass it or extract from principal)
    	Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

	      if (authentication == null || !authentication.isAuthenticated()) {
	            throw new RuntimeException("User is not authenticated. Please login first.");
	      }

	    String username = authentication.getName();
	    
		Long userId1 = userClient.getUserIdByUsername(username);
		if(userId1==null) {
			throw new RuntimeException("not get userId in Collabresource join session method");
		}
        Integer userId = userId1.intValue(); 

        String role = (request != null && request.getRole() != null) ? request.getRole() : "EDITOR";
        String password = (request != null) ? request.getPassword() : null;

        Participant participant = collabService.joinSession(sessionId, userId, role, password);

        // Return summary to the user who just joined
        ParticipantSummary summary = ParticipantSummary.builder()
                .userId(participant.getUserId())
                .role(participant.getRole())
                .color(participant.getColor())
                .cursorLine(participant.getCursorLine())
                .cursorCol(participant.getCursorCol())
                .joinedAt(participant.getJoinedAt())
                .build();

        return ResponseEntity.ok(summary);
    }
    
    //leave the session
    @PostMapping("/{sessionId}/leave")
    public ResponseEntity<String> leaveSession(
            @PathVariable String sessionId) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User is not authenticated");
        }

        String username = authentication.getName();
        Long userIdLong = userClient.getUserIdByUsername(username);
        Integer userId = userIdLong != null ? userIdLong.intValue() : null;

        if (userId == null) {
            throw new RuntimeException("Could not retrieve userId");
        }

        collabService.leaveSession(sessionId, userId);

        return ResponseEntity.ok("Successfully left the session");
    }
    
    
    //end the session
    @PostMapping("/{sessionId}/end")
    public ResponseEntity<String> endSession(
            @PathVariable String sessionId) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User is not authenticated");
        }

        String username = authentication.getName();
        Long userIdLong = userClient.getUserIdByUsername(username);
        Integer ownerId = userIdLong != null ? userIdLong.intValue() : null;

        if (ownerId == null) {
            throw new RuntimeException("Could not retrieve userId");
        }

        collabService.endSession(sessionId, ownerId);

        return ResponseEntity.ok("Session ended successfully");
    }
    
    //get the active list of participant 
    @GetMapping("/{sessionId}/participants")
    public ResponseEntity<List<Participant>> getParticipants(@PathVariable String sessionId) {

        List<Participant> participants = collabService.getParticipants(sessionId);

        return ResponseEntity.ok(participants);
    }
    
    //update the cursor
    @PutMapping("/{sessionId}/cursor")
    public ResponseEntity<String> updateCursor(@PathVariable String sessionId,
                                                @RequestBody UpdateCursorRequest request) {
        collabService.updateCursor(
                sessionId,
                request.getUserId(),
                request.getCursorLine(),
                request.getCursorCol()
        );
        return ResponseEntity.ok("Cursor updated successfully");
    }
    
    
    
}