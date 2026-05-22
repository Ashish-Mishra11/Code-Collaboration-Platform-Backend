package com.codesync.apigateway.configuration;


import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.codesync.apigateway.util.JwtService;

import io.jsonwebtoken.Claims;
import reactor.core.publisher.Mono;

@Component
public class GlobalSecurityConfiguration implements GlobalFilter, Ordered {

    private final JwtService jwtUtils;

    public GlobalSecurityConfiguration(JwtService jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    public int getOrder() {
        return -1; // High priority
    }

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login"
            ,"/api/auth/register"
            ,"/api/auth/forgot-password"
            ,"/api/auth/reset-password"
			,"/oauth2/"
			,"/login/oauth2/"
			,"/api/auth/register/dev"
//			,"/api/auth/admin/approve"
//			,"/api/auth/admin/reject"
			,"/api/auth/approvedeveloper",
			"/api/admin/login",
            // ── Swagger / OpenAPI – no JWT needed ──────────────────────────
            "/swagger-ui",
            "/swagger-ui.html",
            "/v3/api-docs",
            "/webjars/"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        System.out.println("Global Filter Triggered");

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().toString();

        // Allow public endpoints
        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        // Get Authorization header
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("Missing or invalid Authorization header: " + path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        Claims claims;
        try {
            claims = jwtUtils.validateTokenAndGetClaims(token);
        } catch (Exception e) {
            System.out.println("JWT validation failed: " + e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        System.out.println("Claims fetched successfully");

        // Check expiration
        if (jwtUtils.isTokenExpired(claims)) {
            System.out.println("JWT expired");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // Extract username, role, and userId
        String username = claims.getSubject();
        String role = claims.get("role", String.class);
        Object userIdObj = claims.get("userId") != null ? claims.get("userId") : claims.get("adminId");
        String userId = userIdObj != null ? String.valueOf(userIdObj) : null;

        if (username == null || role == null) {
            System.out.println("Missing username or role in token");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        System.out.println("Username: " + username + ", Role: " + role + ", UserId: " + userId);

        // Add headers to downstream request
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("Authorization", authHeader) 
                .header("X-Username", username)
                .header("X-Role", role)
                .header("X-User-Id", userId != null ? userId : "")
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

        System.out.println("Request mutated successfully - proceeding to downstream service");
        return chain.filter(mutatedExchange);
    }
}