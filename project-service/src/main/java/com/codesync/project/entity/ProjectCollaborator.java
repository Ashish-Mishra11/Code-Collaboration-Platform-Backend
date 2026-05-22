package com.codesync.project.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"projectId", "userId"})
})
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectCollaborator {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    private Integer projectId;
    private Integer userId;
    
    // "PROJECT_OWNER" or "COLLABORATOR"
    private String role; 
    
    private LocalDateTime addedAt;
}
