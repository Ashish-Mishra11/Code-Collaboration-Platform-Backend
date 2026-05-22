package com.versionservice.component;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class UserContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String username = request.getHeader("X-Username");
        String role = request.getHeader("X-Role");
        String authHeader = request.getHeader("Authorization");

        if (username != null && !username.trim().isEmpty() && role != null && !role.trim().isEmpty()) {

            // Convert role to Spring Security format (ROLE_Developer, ROLE_USER, etc.)
            String roleWithPrefix = role.toUpperCase().startsWith("ROLE_") 
                                    ? role.toUpperCase() 
                                    : "ROLE_" + role.toUpperCase();

            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority(roleWithPrefix)
            );

            // Create authenticated object
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, authHeader, authorities);

            // Set in SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);

            System.out.println("UserContextFilter: User authenticated → " + username + " | Role: " + role);
        } else {
            System.out.println(" UserContextFilter: Missing X-Username or X-Role headers");
            // You can uncomment below line if you want to strictly reject requests without headers
            // response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            // return;
        }

        // Continue with the request
        filterChain.doFilter(request, response);
    }
}