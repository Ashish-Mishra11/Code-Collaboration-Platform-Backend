package com.codesync.project.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.codesync.project.entity.Project;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Integer> {
    List<Project> findByOwnerId(Integer ownerId);
    Optional<Project> findByProjectId(Integer projectId);
    List<Project> findByVisibility(String visibility);
    List<Project> findByLanguage(String language);
    List<Project> findByNameContainingIgnoreCase(String name);
    // Find projects where user is a member (handled via relation later)
    // Placeholder (will need mapping or custom query)

    List<Project> findByIsArchived(Boolean isArchived);
    Integer countByOwnerId(Integer ownerId);
    
}