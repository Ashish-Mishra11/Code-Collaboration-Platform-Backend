package com.codesync.collab.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.codesync.collab.entity.CollabSession;

@Repository
public interface CollabRepository extends JpaRepository<CollabSession, String> {

    Optional<CollabSession> findBySessionId(String sessionId);

    List<CollabSession> findByProjectId(Integer projectId);

    List<CollabSession> findByFileId(int fileId);

    List<CollabSession> findByOwnerId(int ownerId);
    
    
}