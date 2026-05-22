package com.versionservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "snapshotId")
public class Snapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer snapshotId;        // Changed to Integer (common for JPA)

    private Integer projectId;
    private Integer fileId;
    private Integer authorId;

    private String message;            // commit message
    @Column(length = 5000)             // adjust size as needed
    private String content;            // full file text

    private String hash;               // SHA-256
    private Integer parentSnapshotId;
    private String branch;
    private String tag;

    private LocalDateTime createdAt;

    // Optional: helper method
    public boolean isInitial() {
        return parentSnapshotId == null;
    }
}