package com.codesync.auth.service.implementation;

import com.codesync.auth.dto.*;
import com.codesync.auth.entity.User;
import com.codesync.auth.exceptionhandler.EmailAlreadyExistsException;
import com.codesync.auth.exceptionhandler.UserNotFoundException;
import com.codesync.auth.exceptionhandler.UsernameAlreadyExistsException;
import com.codesync.auth.kafka.AuthNotificationPublisher;
import com.codesync.auth.repository.DeveloperApplication;
import com.codesync.auth.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceImplTest {

    @InjectMocks
    private AuthServiceImpl authServiceImpl;

    @Mock private UserRepository userRepository;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private ModelMapper modelMapper;
    @Mock private DeveloperApplication developerApplication;
    @Mock private AuthNotificationPublisher notificationPublisher;

    private User sampleUser() {
        User u = new User();
        u.setUserId(1);
        u.setUserName("testuser");
        u.setEmail("test@example.com");
        u.setFullName("Test User");
        u.setRole("USER");
        u.setProvider("LOCAL");
        u.setIsActive(true);
        u.setPasswordHash("$2a$12$hashedpassword");
        return u;
    }

    // ── register ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register – succeeds for new user with USER role")
    void register_newUser_returnsResponseDto() {
        RegisterUserDto dto = new RegisterUserDto();
        dto.setUserName("testuser");
        dto.setEmail("test@example.com");
        dto.setFullName("Test User");
        dto.setPassword("password123");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.existsByUserName("testuser")).thenReturn(false);
        when(developerApplication.findByEmail("test@example.com")).thenReturn(Optional.empty());

        User savedUser = sampleUser();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        RegisterResponseDto result = authServiceImpl.register(dto);

        assertThat(result).isNotNull();
        assertThat(result.getUserName()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getRole()).isEqualTo("USER");
    }

    @Test
    @DisplayName("register – throws EmailAlreadyExistsException if email taken")
    void register_duplicateEmail_throwsException() {
        RegisterUserDto dto = new RegisterUserDto();
        dto.setEmail("test@example.com");
        dto.setUserName("newuser");
        dto.setPassword("pass");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authServiceImpl.register(dto))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register – throws UsernameAlreadyExistsException if username taken")
    void register_duplicateUsername_throwsException() {
        RegisterUserDto dto = new RegisterUserDto();
        dto.setEmail("new@example.com");
        dto.setUserName("testuser");
        dto.setPassword("pass");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.existsByUserName("testuser")).thenReturn(true);

        assertThatThrownBy(() -> authServiceImpl.register(dto))
                .isInstanceOf(UsernameAlreadyExistsException.class)
                .hasMessageContaining("Username already exists");
    }

    @Test
    @DisplayName("register – assigns DEVELOPER role when application is APPROVED")
    void register_approvedDeveloper_assignsDeveloperRole() {
        RegisterUserDto dto = new RegisterUserDto();
        dto.setUserName("devuser");
        dto.setEmail("dev@example.com");
        dto.setFullName("Dev User");
        dto.setPassword("pass123");

        when(userRepository.existsByEmail("dev@example.com")).thenReturn(false);
        when(userRepository.existsByUserName("devuser")).thenReturn(false);

        com.codesync.auth.entity.DeveloperApplicationReceived devApp =
                mock(com.codesync.auth.entity.DeveloperApplicationReceived.class);
        when(devApp.getStatus()).thenReturn("APPROVED");
        when(developerApplication.findByEmail("dev@example.com")).thenReturn(Optional.of(devApp));

        User savedUser = sampleUser();
        savedUser.setRole("DEVELOPER");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        RegisterResponseDto result = authServiceImpl.register(dto);
        assertThat(result.getRole()).isEqualTo("DEVELOPER");
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login – returns JWT token on successful authentication")
    void login_validCredentials_returnsToken() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);

        User user = sampleUser();
        when(userRepository.findByUserName("testuser")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token-abc");
        doNothing().when(notificationPublisher).publishUserLogin(any());

        String token = authServiceImpl.login("testuser", "password");

        assertThat(token).isEqualTo("jwt-token-abc");
        verify(jwtService).generateToken(user);
    }

    @Test
    @DisplayName("login – returns 'Login Failed' when authentication fails")
    void login_invalidCredentials_returnsLoginFailed() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);
        when(authenticationManager.authenticate(any())).thenReturn(auth);

        String result = authServiceImpl.login("testuser", "wrongpass");

        assertThat(result).isEqualTo("Login Failed");
        verify(jwtService, never()).generateToken(any());
    }

    // ── getUserIdByUsername ───────────────────────────────────────────────────

    @Test
    @DisplayName("getUserIdByUsername – returns userId when user found")
    void getUserIdByUsername_found_returnsId() {
        when(userRepository.findByUserName("testuser")).thenReturn(Optional.of(sampleUser()));
        Integer id = authServiceImpl.getUserIdByUsername("testuser");
        assertThat(id).isEqualTo(1);
    }

    @Test
    @DisplayName("getUserIdByUsername – returns null when user not found")
    void getUserIdByUsername_notFound_returnsNull() {
        when(userRepository.findByUserName("ghost")).thenReturn(Optional.empty());
        assertThat(authServiceImpl.getUserIdByUsername("ghost")).isNull();
    }

    // ── logout ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("logout – completes without exception")
    void logout_doesNotThrow() {
        assertThatCode(() -> authServiceImpl.logout("some-token")).doesNotThrowAnyException();
    }

    // ── getUserById ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getUserById – returns profile DTO when user exists")
    void getUserById_found_returnsProfile() {
        User user = sampleUser();
        UserProfileDto profileDto = new UserProfileDto();
        profileDto.setUserName("testuser");

        when(userRepository.findByUserId(1)).thenReturn(Optional.of(user));
        when(modelMapper.map(user, UserProfileDto.class)).thenReturn(profileDto);

        UserProfileDto result = authServiceImpl.getUserById(1);
        assertThat(result.getUserName()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("getUserById – throws UserNotFoundException when not found")
    void getUserById_notFound_throwsException() {
        when(userRepository.findByUserId(999)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authServiceImpl.getUserById(999))
                .isInstanceOf(UserNotFoundException.class);
    }

    // ── updateProfile ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateProfile – updates all fields when non-null")
    void updateProfile_allFieldsPresent_updatesUser() {
        User user = sampleUser();
        UpdateUserProfileDto dto = new UpdateUserProfileDto();
        dto.setFullName("Updated Name");
        dto.setAvatarUrl("http://avatar.url/img.png");
        dto.setBio("New bio");

        UserProfileDto returnDto = new UserProfileDto();
        returnDto.setFullName("Updated Name");

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(modelMapper.map(any(User.class), eq(UserProfileDto.class))).thenReturn(returnDto);

        UserProfileDto result = authServiceImpl.updateProfile(1, dto);

        assertThat(result.getFullName()).isEqualTo("Updated Name");
        verify(userRepository).save(argThat(u ->
                "Updated Name".equals(u.getFullName()) &&
                "http://avatar.url/img.png".equals(u.getAvatarUrl()) &&
                "New bio".equals(u.getBio())
        ));
    }

    @Test
    @DisplayName("updateProfile – throws UserNotFoundException when user not found")
    void updateProfile_notFound_throwsException() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authServiceImpl.updateProfile(999, new UpdateUserProfileDto()))
                .isInstanceOf(UserNotFoundException.class);
    }

    // ── changePassword ────────────────────────────────────────────────────────

    @Test
    @DisplayName("changePassword – saves new hashed password when old password is correct")
    void changePassword_correctOldPassword_savesNew() {
        User user = sampleUser();
        // Encode a known password so BCrypt.matches works
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder enc =
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(12);
        user.setPasswordHash(enc.encode("oldpass"));

        ChangePasswordDto dto = new ChangePasswordDto();
        dto.setOldPassword("oldpass");
        dto.setNewPassword("newpass123");

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        assertThatCode(() -> authServiceImpl.changePassword(1, dto)).doesNotThrowAnyException();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("changePassword – throws RuntimeException when old password is wrong")
    void changePassword_wrongOldPassword_throwsException() {
        User user = sampleUser();
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder enc =
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(12);
        user.setPasswordHash(enc.encode("correctpass"));

        ChangePasswordDto dto = new ChangePasswordDto();
        dto.setOldPassword("wrongpass");
        dto.setNewPassword("newpass123");

        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authServiceImpl.changePassword(1, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Old password is incorrect");
    }

    // ── searchUsers ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("searchUsers – returns null (stub)")
    void searchUsers_returnsNull() {
        assertThat(authServiceImpl.searchUsers("any")).isNull();
    }
}
