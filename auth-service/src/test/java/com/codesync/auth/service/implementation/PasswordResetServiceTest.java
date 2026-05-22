package com.codesync.auth.service.implementation;

import com.codesync.auth.entity.PasswordResetToken;
import com.codesync.auth.entity.User;
import com.codesync.auth.exceptionhandler.UserNotFoundException;
import com.codesync.auth.repository.PasswordResetTokenRepository;
import com.codesync.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User user;
    private PasswordResetToken token;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("test@example.com");

        token = new PasswordResetToken();
        token.setToken("sample-token-123");
        token.setUser(user);
        token.setExpiryDate(LocalDateTime.now().plusMinutes(15));
    }

    @Test
    void testForgotPassword_UserExists() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(tokenRepository.findByUser(user)).thenReturn(Optional.empty());

        passwordResetService.forgotPassword("test@example.com");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        assertNotNull(tokenCaptor.getValue().getToken());
        assertEquals(user, tokenCaptor.getValue().getUser());

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mailCaptor.capture());
        assertEquals("test@example.com", mailCaptor.getValue().getTo()[0]);
        assertTrue(mailCaptor.getValue().getText().contains(tokenCaptor.getValue().getToken()));
    }

    @Test
    void testForgotPassword_UserExistsWithExistingToken() {
        PasswordResetToken existingToken = new PasswordResetToken();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(tokenRepository.findByUser(user)).thenReturn(Optional.of(existingToken));

        passwordResetService.forgotPassword("test@example.com");

        verify(tokenRepository).delete(existingToken);
        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void testForgotPassword_UserNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> {
            passwordResetService.forgotPassword("unknown@example.com");
        });

        verify(tokenRepository, never()).save(any());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void testValidateAndFetch_ValidToken() {
        when(tokenRepository.findByToken("sample-token-123")).thenReturn(Optional.of(token));

        PasswordResetToken fetchedToken = passwordResetService.validateAndFetch("sample-token-123");

        assertEquals(token, fetchedToken);
    }

    @Test
    void testValidateAndFetch_InvalidToken() {
        when(tokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            passwordResetService.validateAndFetch("invalid-token");
        });

        assertEquals("Token is invalid or has already been used", exception.getMessage());
    }

    @Test
    void testValidateAndFetch_ExpiredToken() {
        token.setExpiryDate(LocalDateTime.now().minusMinutes(1)); // Expired
        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            passwordResetService.validateAndFetch("expired-token");
        });

        assertEquals("Token has expired. Please request a new one", exception.getMessage());
        verify(tokenRepository).delete(token);
    }

    @Test
    void testDeleteToken() {
        passwordResetService.deleteToken(token);
        verify(tokenRepository).delete(token);
    }
}
