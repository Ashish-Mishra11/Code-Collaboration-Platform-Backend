package com.codesync.auth.service.implementation;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.codesync.auth.entity.PasswordResetToken;
import com.codesync.auth.entity.User;
import com.codesync.auth.exceptionhandler.UserNotFoundException;
import com.codesync.auth.repository.PasswordResetTokenRepository;
import com.codesync.auth.repository.UserRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class PasswordResetService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;

    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("No account found with that email"));

        
        // Delete any existing token for this user before creating a new one
        tokenRepository.findByToken(email).ifPresent(tokenRepository::delete);

        String rawToken = UUID.randomUUID().toString();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(rawToken);
        resetToken.setUser(user);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(15));
        tokenRepository.save(resetToken);

        sendResetEmail(user.getEmail(), rawToken);
    }

    public PasswordResetToken validateAndFetch(String token) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token is invalid or has already been used"));

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(resetToken);
            throw new RuntimeException("Token has expired. Please request a new one");
        }

        return resetToken;
    }

    public void deleteToken(PasswordResetToken token) {
        tokenRepository.delete(token);
    }

    private void sendResetEmail(String to, String token) {
        String resetLink = "http://localhost:3000/reset-password?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Reset your password");
        message.setText("Click the link below to reset your password (valid for 15 minutes):\n\n" + resetLink
                + "\n\nIf you did not request this, ignore this email.");
        mailSender.send(message);
    }
}