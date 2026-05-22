package com.codesync.auth.component;

import com.codesync.auth.dto.AdminLoginDto;
import com.codesync.auth.dto.DeveloperApplicationDto;
import com.codesync.auth.service.implementation.AdminService;
import com.codesync.auth.service.implementation.DeveloperRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminResource {

    private final AdminService adminService;
    private final DeveloperRoleService developerRoleService;

    /**
     * POST /api/admin/login
     * Body: { username, password }
     * Returns: JWT token string
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> adminLogin(@Valid @RequestBody AdminLoginDto dto) {
        String token = adminService.loginAdmin(dto);
        return ResponseEntity.ok(Map.of("token", token, "role", "ADMIN"));
    }

    /**
     * GET /api/admin/applications
     * Returns list of all pending developer applications (no PDF bytes).
     * Requires valid admin JWT.
     */
    @GetMapping("/applications")
    public ResponseEntity<List<DeveloperApplicationDto>> getPendingApplications() {
        return ResponseEntity.ok(adminService.getAllPendingApplications());
    }

    /**
     * POST /api/admin/approve?email=...
     * Approves developer and sends them an email with a registration link.
     */
    @PostMapping("/approve")
    public ResponseEntity<String> approveApplication(@RequestParam String email) {
        return ResponseEntity.ok(developerRoleService.generateMailToDeveloper(email));
    }

    /**
     * POST /api/admin/reject?email=...
     * Rejects and deletes the developer application.
     */
    @PostMapping("/reject")
    public ResponseEntity<String> rejectApplication(@RequestParam String email) {
        return ResponseEntity.ok(developerRoleService.rejectHandler(email));
    }
}
