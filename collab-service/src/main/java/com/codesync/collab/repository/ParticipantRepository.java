package com.codesync.collab.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.codesync.collab.entity.Participant;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant,Long> {

	Optional<Participant> findBySession_SessionIdAndUserId(String sessionId, Integer userId);
}
