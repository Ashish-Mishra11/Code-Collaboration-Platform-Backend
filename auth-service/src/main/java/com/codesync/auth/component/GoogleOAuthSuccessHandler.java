package com.codesync.auth.component;

import com.codesync.auth.entity.User;
import com.codesync.auth.repository.UserRepository;
import com.codesync.auth.service.implementation.JwtService;

import jakarta.servlet.http.Cookie;
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
import java.util.UUID;

@Component
public class GoogleOAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    public GoogleOAuthSuccessHandler(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        // Google returns these attributes
        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");
        String picture = oauthUser.getAttribute("picture");   // profile picture

     // Fallback: GitHub may return null email if user keeps it private
        if (email == null) email = name + "@google.com";

        final String finalEmail = email;

        // Check if user exists, if not create new
        User user = userRepository.findByEmail(finalEmail).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(finalEmail);
            newUser.setUserName(name+"_"+UUID.randomUUID());   // Use part before @ as username
            newUser.setFullName(name);
            newUser.setAvatarUrl(picture);
            newUser.setProvider("GOOGLE");
            newUser.setRole("USER");
            newUser.setIsActive(true);
            newUser.setCreateAt(LocalDateTime.now());
            return userRepository.save(newUser);
        });

        // Generate JWT token using userID
        String jwt = jwtService.generateToken(user);

     // GoogleOAuthSuccessHandler.java
        String frontendUrl = "http://localhost:3002/login"
                + "?token=" + jwt
                + "&provider=GOOGLE"
                + "&username=" + URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8);

        response.sendRedirect(frontendUrl);
    }
}