package com.codesync.file.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "project-service")
public interface ProjectServiceClient {

    /**
     * Returns the role of a user in a project: "OWNER", "COLLABORATOR", or "NONE"
     */
    @GetMapping("/api/project/{projectId}/role/{userId}")
    RoleResponse getUserRole(@PathVariable Integer projectId, @PathVariable Integer userId);

    class RoleResponse {
        public String role;
    }
}
