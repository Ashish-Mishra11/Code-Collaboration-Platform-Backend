package com.codesync.auth.component;

import com.codesync.auth.entity.User;
import com.codesync.auth.repository.UserRepository;
import com.codesync.auth.service.implementation.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Component
public class GitHubOAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final com.codesync.auth.kafka.AuthNotificationPublisher notificationPublisher;
    private final com.codesync.auth.repository.DeveloperApplication developerApplication;

    public GitHubOAuthSuccessHandler(UserRepository userRepository, JwtService jwtService, com.codesync.auth.kafka.AuthNotificationPublisher notificationPublisher, com.codesync.auth.repository.DeveloperApplication developerApplication) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.notificationPublisher = notificationPublisher;
        this.developerApplication = developerApplication;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        String email    = oauthUser.getAttribute("email");
        String login    = oauthUser.getAttribute("login");   // GitHub username
        String name     = oauthUser.getAttribute("name");
        String avatar   = oauthUser.getAttribute("avatar_url");

        // Fallback: GitHub may return null email if user keeps it private
        if (email == null) email = login + "@github.com";

        final String finalEmail = email;
        final String finalLogin = login;

        User user = userRepository.findByEmail(finalEmail).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(finalEmail);
            newUser.setUserName(finalLogin);
            newUser.setFullName(name);
            newUser.setAvatarUrl(avatar);
            newUser.setProvider("GITHUB");

            java.util.Optional<com.codesync.auth.entity.DeveloperApplicationReceived> devApp = developerApplication.findByEmail(finalEmail);
            if(devApp.isPresent() && "APPROVED".equals(devApp.get().getStatus())) {
                newUser.setRole("DEVELOPER");
            } else {
                newUser.setRole("USER");
            }

            newUser.setIsActive(true);
            newUser.setCreateAt(LocalDateTime.now());
            // passwordHash left null — GitHub users won't use local login
            return userRepository.save(newUser);
        });

        // If user already exists but their application was approved later, upgrade their role
        java.util.Optional<com.codesync.auth.entity.DeveloperApplicationReceived> existingDevApp = developerApplication.findByEmail(finalEmail);
        if ("USER".equals(user.getRole()) && existingDevApp.isPresent() && "APPROVED".equals(existingDevApp.get().getStatus())) {
            user.setRole("DEVELOPER");
            userRepository.save(user);
        }

        
        //generating jwt using userID
        String jwt = jwtService.generateToken(user);

        // Publish login event to Kafka
        try {
            com.codesync.auth.kafka.UserLoginEvent loginEvent = com.codesync.auth.kafka.UserLoginEvent.builder()
                    .userId(user.getUserId())
                    .userName(user.getUserName())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .loginTime(LocalDateTime.now())
                    .build();
            notificationPublisher.publishUserLogin(loginEvent);
        } catch (Exception ex) {
            System.out.println("[Kafka] Could not publish GitHub login event: " + ex.getMessage());
        }

        // Redirect to frontend with token as query param
        // Change this URL to wherever your frontend lives
        // ✅ Return token as JSON (instead of redirect)
     // GitHubOAuthSuccessHandler.java  
        String frontendUrl = "http://localhost:3000/oauth/callback"
                + "?token=" + jwt;

        response.sendRedirect(frontendUrl);
        
    }
}