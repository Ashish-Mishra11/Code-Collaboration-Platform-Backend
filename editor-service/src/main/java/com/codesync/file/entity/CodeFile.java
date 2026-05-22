package com.codesync.file.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "code_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer fileId;

    @Column(nullable = false)
    private Integer projectId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 1000)
    private String path;

    private String language;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    private Long size;

    @Column(nullable = false)
    private Integer createdById;

    private Integer lastEditedBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Boolean isDeleted;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
        if (this.content == null) {
            this.content = "";
        }
        if (this.size == null) {
            this.size = (long) this.content.getBytes().length;
        }
        if (this.lastEditedBy == null) {
            this.lastEditedBy = this.createdById;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.content == null) {
            this.content = "";
        }
        this.size = (long) this.content.getBytes().length;
    }
}