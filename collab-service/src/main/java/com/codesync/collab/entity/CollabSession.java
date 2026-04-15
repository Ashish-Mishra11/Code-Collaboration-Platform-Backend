package com.codesync.collab.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollabSession {

    @Id
    private String sessionId; // UUID

    private Integer projectId;
    private Integer fileId;
    private Integer ownerId;

    private String status; // ACTIVE / ENDED
    private String language;

    private LocalDateTime createdAt;
    private LocalDateTime endedAt;

    private Integer maxParticipants;
    private Boolean isPasswordProtected;
    private String sessionPassword;
    
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL)
    private List<Participant> participants = new ArrayList<>();
}