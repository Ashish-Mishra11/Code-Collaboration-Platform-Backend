package com.codesync.auth.service.implementation;

import com.codesync.auth.dto.AdminLoginDto;
import com.codesync.auth.dto.DeveloperApplicationDto;
import com.codesync.auth.entity.Admin;
import com.codesync.auth.repository.AdminRepository;
import com.codesync.auth.repository.DeveloperApplication;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;
    private final DeveloperApplication developerApplicationRepository;
    private final JwtService jwtService;

    /**
     * Validates admin credentials and returns a JWT with role=ADMIN.
     */
    public String loginAdmin(AdminLoginDto dto) {
        Admin admin = adminRepository.findByUsername(dto.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid admin credentials"));

        if (!new BCryptPasswordEncoder(12).matches(dto.getPassword(), admin.getPasswordHash())) {
            throw new RuntimeException("Invalid admin credentials");
        }

        return jwtService.generateAdminToken(admin);
    }

    /**
     * Returns all pending developer applications (without the resume blob).
     */
    public List<DeveloperApplicationDto> getAllPendingApplications() {
        return developerApplicationRepository.findByStatus("PENDING").stream()
                .map(app -> new DeveloperApplicationDto(
                        app.getId(),
                        app.getName(),
                        app.getEmail(),
                        app.getCompanyName(),
                        app.getCreatedAt()))
                .collect(Collectors.toList());
    }
}
