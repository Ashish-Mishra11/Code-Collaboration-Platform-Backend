package com.codesync.collab.configuration;

import com.codesync.collab.component.UserContextFilter;
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

            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                // WebSocket handshake endpoints must be public (SockJS uses HTTP upgrade)
                .requestMatchers("/collab/**", "/collab-ws/**").permitAll()
                // Actuator / docs
                .requestMatchers(
                    "/public/**",
                    "/actuator/health",
                    "/actuator/info",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()
                // All REST APIs require authenticated users
                .anyRequest().authenticated()
            )
            .addFilterBefore(userContextFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
