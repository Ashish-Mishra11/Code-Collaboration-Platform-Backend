package com.codesync.collab.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor 
@AllArgsConstructor
public class Participant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long participantId;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private CollabSession session;

    private Integer userId;
    private String role;           // Developer,Project manager(further I we develop more),No user here 
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;

    private Integer cursorLine = 0;
    private Integer cursorCol = 0;
    private String color;          // unique hex color per session
}