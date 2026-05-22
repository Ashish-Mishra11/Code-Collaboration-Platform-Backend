package com.codesync.project.repository;

import com.codesync.project.entity.ProjectCollaborator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectCollaboratorRepository extends JpaRepository<ProjectCollaborator, Integer> {
    List<ProjectCollaborator> findByProjectId(Integer projectId);
    Optional<ProjectCollaborator> findByProjectIdAndUserId(Integer projectId, Integer userId);
    int countByProjectId(Integer projectId);
    void deleteByProjectIdAndUserId(Integer projectId, Integer userId);
}
