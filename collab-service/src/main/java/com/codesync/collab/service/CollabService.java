package com.codesync.collab.service;

import java.util.List;
import java.util.Optional;
import com.codesync.collab.dto.*;
import com.codesync.collab.entity.CollabSession;
import com.codesync.collab.entity.Participant;

public interface CollabService {

	CreateCollabSessionResponse createSession(CreateCollabSessionRequest request);

    Optional<CreateCollabSessionResponse> getSessionById(String sessionId);

    List<CreateCollabSessionResponse> getSessionsByProject(Integer projectId);

    Participant joinSession(String sessionId, Integer userId, String role, String password);

    void leaveSession(String sessionId, Integer userId);

    void endSession(String sessionId, Integer ownerId);

    List<Participant> getParticipants(String sessionId);

    void updateCursor(String sessionId, Integer userId, int line, int col);

//    void broadcastChange(String sessionId, String change);

}