package com.codesync.auth.service.implementation;

import com.codesync.auth.dto.AdminLoginDto;
import com.codesync.auth.dto.DeveloperApplicationDto;
import com.codesync.auth.entity.Admin;
import com.codesync.auth.entity.DeveloperApplicationReceived;
import com.codesync.auth.repository.AdminRepository;
import com.codesync.auth.repository.DeveloperApplication;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService Unit Tests")
class AdminServiceTest {

    @Mock private AdminRepository adminRepository;
    @Mock private DeveloperApplication developerApplicationRepository;
    @Mock private JwtService jwtService;

    @InjectMocks private AdminService adminService;

    private Admin buildAdmin(String rawPassword) {
        Admin a = new Admin();
        a.setAdminId(1);
        a.setUsername("admin");
        a.setPasswordHash(new BCryptPasswordEncoder(12).encode(rawPassword));
        return a;
    }

    // ── loginAdmin ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("loginAdmin – returns token for valid credentials")
    void loginAdmin_valid_returnsToken() {
        Admin admin = buildAdmin("secret");
        when(adminRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(jwtService.generateAdminToken(admin)).thenReturn("admin-jwt");

        AdminLoginDto dto = new AdminLoginDto();
        dto.setUsername("admin");
        dto.setPassword("secret");

        String token = adminService.loginAdmin(dto);
        assertThat(token).isEqualTo("admin-jwt");
    }

    @Test
    @DisplayName("loginAdmin – throws RuntimeException for unknown username")
    void loginAdmin_unknownUser_throwsException() {
        when(adminRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        AdminLoginDto dto = new AdminLoginDto();
        dto.setUsername("ghost");
        dto.setPassword("pass");

        assertThatThrownBy(() -> adminService.loginAdmin(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid admin credentials");
    }

    @Test
    @DisplayName("loginAdmin – throws RuntimeException for wrong password")
    void loginAdmin_wrongPassword_throwsException() {
        Admin admin = buildAdmin("correctpass");
        when(adminRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        AdminLoginDto dto = new AdminLoginDto();
        dto.setUsername("admin");
        dto.setPassword("wrongpass");

        assertThatThrownBy(() -> adminService.loginAdmin(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid admin credentials");
    }

    // ── getAllPendingApplications ──────────────────────────────────────────────

    @Test
    @DisplayName("getAllPendingApplications – maps PENDING apps to DTOs")
    void getAllPendingApplications_returnsDtos() {
        DeveloperApplicationReceived app1 = mock(DeveloperApplicationReceived.class);
        when(app1.getId()).thenReturn(1);
        when(app1.getName()).thenReturn("Alice");
        when(app1.getEmail()).thenReturn("alice@example.com");
        when(app1.getCompanyName()).thenReturn("Acme");
        when(app1.getCreatedAt()).thenReturn(LocalDateTime.now());

        DeveloperApplicationReceived app2 = mock(DeveloperApplicationReceived.class);
        when(app2.getId()).thenReturn(2);
        when(app2.getName()).thenReturn("Bob");
        when(app2.getEmail()).thenReturn("bob@example.com");
        when(app2.getCompanyName()).thenReturn("Beta");
        when(app2.getCreatedAt()).thenReturn(LocalDateTime.now());

        when(developerApplicationRepository.findByStatus("PENDING")).thenReturn(List.of(app1, app2));

        List<DeveloperApplicationDto> result = adminService.getAllPendingApplications();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Alice");
        assertThat(result.get(1).getEmail()).isEqualTo("bob@example.com");
    }

    @Test
    @DisplayName("getAllPendingApplications – returns empty list when no pending apps")
    void getAllPendingApplications_empty_returnsEmptyList() {
        when(developerApplicationRepository.findByStatus("PENDING")).thenReturn(List.of());

        List<DeveloperApplicationDto> result = adminService.getAllPendingApplications();
        assertThat(result).isEmpty();
    }
}
