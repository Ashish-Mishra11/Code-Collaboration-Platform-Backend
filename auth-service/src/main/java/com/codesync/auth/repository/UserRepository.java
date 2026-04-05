package com.codesync.auth.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.codesync.auth.entity.User;

public interface UserRepository extends JpaRepository<User, Integer> {

	    Optional<User> findByEmail(String email);

	    Optional<User> findByUserName(String userName);

	    Optional<User> findByUserId(int userId);

	    boolean existsByEmail(String email);

	    boolean existsByUserName(String userName);

	    List<User> findAllByRole(String role);

	    List<User> findByUserNameContainingIgnoreCase(String userName);

	    void deleteByUserId(int userId);
}
