package com.codesync.project.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "auth-service", url = "${auth.service.url:http://localhost:8080}")
public interface UserClient {

    @GetMapping("/api/auth/users/by-username")
    Long getUserIdByUsername(
            
            @RequestParam("username") String username
    );
}