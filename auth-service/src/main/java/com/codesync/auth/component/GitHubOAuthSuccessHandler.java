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
import java.time.LocalDateTime;

@Component
public class GitHubOAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    public GitHubOAuthSuccessHandler(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
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
            newUser.setRole("DEVELOPER");
            newUser.setIsActive(true);
            newUser.setCreateAt(LocalDateTime.now());
            // passwordHash left null — GitHub users won't use local login
            return userRepository.save(newUser);
        });

        
        //generating jwt using userID
        String jwt = jwtService.generateToken(user.getUserId().toString());

        // Redirect to frontend with token as query param
        // Change this URL to wherever your frontend lives
        // ✅ Return token as JSON (instead of redirect)
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String jsonResponse = String.format(
        		 "{\"token\": \"%s\", \"username\": \"%s\", \"provider\": \"GITHUB\"}",
            jwt,
            user.getEmail()
        );

        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}