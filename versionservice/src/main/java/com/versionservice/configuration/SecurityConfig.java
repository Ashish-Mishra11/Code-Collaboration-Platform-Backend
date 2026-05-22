package com.versionservice.configuration;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.versionservice.component.UserContextFilter;



@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserContextFilter userContextFilter;

    public SecurityConfig(UserContextFilter userContextFilter) {
        this.userContextFilter = userContextFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/public/**",
                                         "/actuator/health",
                                         "/actuator/info",
                                         "/swagger-ui/**",
                                         "/v3/api-docs/**").permitAll()

                        // Role-based access control for Versioning
                        // Only developers can save versions, restore, branch, tag
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/versions/**").hasRole("DEVELOPER")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/versions/**").hasRole("DEVELOPER")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/versions/**").hasRole("DEVELOPER")

                        // Read access (GET) is allowed for any authenticated user (so USER can view history)
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/versions/**").authenticated()

                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )

                // Add our custom filter before Spring's default authentication filter
                .addFilterBefore(userContextFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}