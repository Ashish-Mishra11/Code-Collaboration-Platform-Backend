package com.codesync.auth.service.implementation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.multipart.MultipartFile;
import jakarta.mail.internet.MimeMessage;
import com.codesync.auth.dto.RegisterDeveloperDto;
import com.codesync.auth.entity.DeveloperApplicationReceived;
import com.codesync.auth.kafka.AuthNotificationPublisher;
import com.codesync.auth.kafka.DeveloperApprovedEvent;
import com.codesync.auth.repository.DeveloperApplication;

import java.util.Optional;
import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class DeveloperRoleServiceTest {

    @InjectMocks
    private DeveloperRoleService developerRoleService;

    @Mock
    private DeveloperApplication developerApplication;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private AuthNotificationPublisher notificationPublisher;

    @Mock
    private MultipartFile mockResume;

    @Test
    void testRegisterDeveloperApplications_AlreadyExists() {
        RegisterDeveloperDto dto = new RegisterDeveloperDto();
        dto.setEmail("dev@test.com");
        when(developerApplication.findByEmail("dev@test.com")).thenReturn(Optional.of(new DeveloperApplicationReceived()));
        
        String result = developerRoleService.registerDeveloperApplications(dto);
        assertEquals("Your Application already exists in the database. Please wait for admin approval.", result);
    }

    @Test
    void testRegisterDeveloperApplications_EmptyResume() {
        RegisterDeveloperDto dto = new RegisterDeveloperDto();
        dto.setEmail("new@test.com");
        dto.setResume(mockResume);
        when(developerApplication.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(mockResume.isEmpty()).thenReturn(true);
        
        String result = developerRoleService.registerDeveloperApplications(dto);
        assertEquals("Resume file is empty", result);
    }

    @Test
    void testRegisterDeveloperApplications_InvalidPdf() {
        RegisterDeveloperDto dto = new RegisterDeveloperDto();
        dto.setEmail("new@test.com");
        dto.setResume(mockResume);
        when(developerApplication.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(mockResume.isEmpty()).thenReturn(false);
        when(mockResume.getContentType()).thenReturn("image/jpeg");
        
        String result = developerRoleService.registerDeveloperApplications(dto);
        assertEquals("Only PDF files are allowed", result);
    }

    @Test
    void testRegisterDeveloperApplications_Success() throws Exception {
        RegisterDeveloperDto dto = new RegisterDeveloperDto();
        dto.setEmail("new@test.com");
        dto.setName("John Dev");
        dto.setCompanyName("Tech Corp");
        dto.setResume(mockResume);
        
        when(developerApplication.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(mockResume.isEmpty()).thenReturn(false);
        when(mockResume.getContentType()).thenReturn("application/pdf");
        when(mockResume.getBytes()).thenReturn(new byte[]{1,2,3});
        when(mockResume.getOriginalFilename()).thenReturn("resume.pdf");
        
        MimeMessage mockMimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mockMimeMessage);
        
        String result = developerRoleService.registerDeveloperApplications(dto);
        
        verify(developerApplication).save(any(DeveloperApplicationReceived.class));
        verify(mailSender).send(mockMimeMessage);
        assertEquals("Your Application Registered successfully — now wait for Admin approval.", result);
    }
    
    @Test
    void testGenerateMailToDeveloper_KafkaSuccess() {
        DeveloperApplicationReceived app = new DeveloperApplicationReceived();
        app.setName("John");
        app.setCompanyName("Acme");
        
        when(developerApplication.findByEmail("john@test.com")).thenReturn(Optional.of(app));
        doNothing().when(notificationPublisher).publishDeveloperApproved(any(DeveloperApprovedEvent.class));
        
        String result = developerRoleService.generateMailToDeveloper("john@test.com");
        
        verify(notificationPublisher).publishDeveloperApproved(any(DeveloperApprovedEvent.class));
        verify(developerApplication, times(3)).findByEmail("john@test.com");
        verify(developerApplication).save(any(DeveloperApplicationReceived.class));
        assertEquals("Approval mail dispatched to the developer.", result);
    }
    
    @Test
    void testGenerateMailToDeveloper_KafkaFailure_Fallback() {
        DeveloperApplicationReceived app = new DeveloperApplicationReceived();
        app.setName("John");
        
        when(developerApplication.findByEmail("john@test.com")).thenReturn(Optional.of(app));
        doThrow(new RuntimeException("Kafka down")).when(notificationPublisher).publishDeveloperApproved(any());
        
        String result = developerRoleService.generateMailToDeveloper("john@test.com");
        
        verify(mailSender).send(any(SimpleMailMessage.class));
        assertEquals("Approval mail dispatched to the developer.", result);
    }

    @Test
    void testRejectHandler_Success() {
        DeveloperApplicationReceived app = new DeveloperApplicationReceived();
        when(developerApplication.findByEmail("john@test.com")).thenReturn(Optional.of(app));
        
        String result = developerRoleService.rejectHandler("john@test.com");
        
        verify(developerApplication).save(app);
        assertEquals("REJECTED", app.getStatus());
        assertEquals("Developer application rejected and updated in database.", result);
    }
    
    @Test
    void testRejectHandler_NotFound() {
        when(developerApplication.findByEmail("john@test.com")).thenReturn(Optional.empty());
        
        String result = developerRoleService.rejectHandler("john@test.com");
        
        assertEquals("No application found for that email.", result);
    }
}
