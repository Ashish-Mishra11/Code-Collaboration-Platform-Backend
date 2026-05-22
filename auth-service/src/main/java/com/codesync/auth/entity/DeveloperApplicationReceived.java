package com.codesync.auth.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
public class DeveloperApplicationReceived {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    private String name;
	@Column(unique = true)
	private String email;
	
	private String companyName;
	// Store Resume as PDF (BLOB)
    @Lob
    @Column(name = "resume_pdf", columnDefinition = "LONGBLOB", nullable = false)
    private byte[] resumePdf;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private String status = "PENDING";
}
