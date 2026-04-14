package com.codesync.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.codesync.auth.entity.DeveloperApplicationReceived;
import com.codesync.auth.entity.User;

public interface DeveloperApplication extends JpaRepository<DeveloperApplicationReceived, Integer> {

	Optional<DeveloperApplicationReceived> findByEmail(String email);
}
