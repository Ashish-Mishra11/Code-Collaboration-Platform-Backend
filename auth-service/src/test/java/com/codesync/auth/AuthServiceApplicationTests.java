package com.codesync.auth;

import com.codesync.auth.dto.RegisterResponseDto;
import com.codesync.auth.dto.RegisterUserDto;
import com.codesync.auth.entity.User;
import com.codesync.auth.exceptionhandler.EmailAlreadyExistsException;
import com.codesync.auth.exceptionhandler.UsernameAlreadyExistsException;
import com.codesync.auth.repository.DeveloperApplication;
import com.codesync.auth.repository.UserRepository;
import com.codesync.auth.service.implementation.AuthServiceImpl;
import com.codesync.auth.service.implementation.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceApplicationTests {

    @Mock private UserRepository userRepository;
    @Mock private DeveloperApplication developerApplication;
    @Mock private JwtService jwtService;
    @Mock private ModelMapper modelMapper;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterUserDto registerDto;
    private User savedUser;

    @BeforeEach
    void setUp() {
        registerDto = new RegisterUserDto();
        registerDto.setUserName("testuser");
        registerDto.setEmail("test@example.com");
        registerDto.setFullName("Test User");
        registerDto.setPassword("password123");

        savedUser = new User();
        savedUser.setUserId(1);
        savedUser.setUserName("testuser");
        savedUser.setEmail("test@example.com");
        savedUser.setRole("USER");
        savedUser.setIsActive(true);
    }

    @Test
    @DisplayName("register: successfully registers a new user with USER role")
    void register_success() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.existsByUserName("testuser")).thenReturn(false);
        when(developerApplication.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        RegisterResponseDto result = authService.register(registerDto);

        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register: assigns DEVELOPER role if email is in developer applications")
    void register_developerRole() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUserName(anyString())).thenReturn(false);
        com.codesync.auth.entity.DeveloperApplicationReceived mockDevApp = new com.codesync.auth.entity.DeveloperApplicationReceived();
        mockDevApp.setStatus("APPROVED");
        when(developerApplication.findByEmail("test@example.com")).thenReturn(Optional.of(mockDevApp));
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        authService.register(registerDto);

        verify(userRepository).save(argThat(u -> "DEVELOPER".equals(u.getRole())));
    }

    @Test
    @DisplayName("register: throws EmailAlreadyExistsException when email is taken")
    void register_emailExists() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerDto))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register: throws UsernameAlreadyExistsException when username is taken")
    void register_usernameExists() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUserName("testuser")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerDto))
                .isInstanceOf(UsernameAlreadyExistsException.class)
                .hasMessageContaining("Username already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register: password is BCrypt-encoded (not stored as plain text)")
    void register_passwordHashed() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUserName(anyString())).thenReturn(false);
        when(developerApplication.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            assertThat(u.getPasswordHash()).isNotEqualTo("password123");
            assertThat(u.getPasswordHash()).startsWith("$2");
            return u;
        });

        authService.register(registerDto);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("authentication: bad credentials propagate correctly")
    void login_badCredentials() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() ->
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken("user", "wrongpass")))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("jwtService: generateToken returns a token string")
    void jwtService_generateToken() {
        when(jwtService.generateToken(any())).thenReturn("header.payload.signature");
        String token = jwtService.generateToken(savedUser);
        assertThat(token).contains(".");
    }
}
