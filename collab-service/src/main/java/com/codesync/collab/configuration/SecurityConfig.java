package com.codesync.collab.configuration;

import com.codesync.collab.component.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;



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
                        // Add your public endpoints here
                        .requestMatchers("/public/**",
                                         "/actuator/health",
                                         "/actuator/info",
                                         "/swagger-ui/**",
                                         "/v3/api-docs/**").permitAll()

                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )

                // Add our custom filter before Spring's default authentication filter
                .addFilterBefore(userContextFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}