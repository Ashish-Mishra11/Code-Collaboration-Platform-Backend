package com.codesync.auth.component;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codesync.auth.dto.ChangePasswordDto;
import com.codesync.auth.dto.LoginUserDto;
import com.codesync.auth.dto.RegisterResponseDto;
import com.codesync.auth.dto.RegisterUserDto;
import com.codesync.auth.dto.UpdateUserProfileDto;
import com.codesync.auth.dto.UserProfileDto;
import com.codesync.auth.entity.User;
import com.codesync.auth.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthResource {

	private AuthService authService;
	
	public AuthResource(AuthService authService) {
		this.authService=authService;
	}
	
    // ---------------- REGISTER ----------------
    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDto> register(@RequestBody RegisterUserDto registerUserDto) {
    	return ResponseEntity.ok(authService.register(registerUserDto));
    	
    }
    // ---------------- LOGIN ----------------
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginUserDto loginUserDto) {
    	return ResponseEntity.ok(authService.login(loginUserDto.getUserName(),loginUserDto.getPassword()));
    	
    }
    
    // ---------------- LOGOUT ----------------
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok("Logged out successfully");
    }
    
    // ---------------- REFRESH TOKEN ----------------
    @PostMapping("/refresh")
    public ResponseEntity<String> refresh(@RequestHeader("Authorization") String token) {

        String newToken = authService.refreshToken(token);

        return ResponseEntity.ok(newToken);
    }
    
    // ---------------- GET PROFILE ----------------
    @GetMapping("/profile/{userId}")
    public ResponseEntity<UserProfileDto> getProfile(@PathVariable Integer userId) {
    	
    	 return ResponseEntity.ok(authService.getUserById(userId));
    }
    
    // ---------------- UPDATE PROFILE ----------------
    @PutMapping("/profile/{userId}")
    public ResponseEntity<UserProfileDto> updateProfile(
            @PathVariable Integer userId,
            @RequestBody UpdateUserProfileDto dto) {

        return ResponseEntity.ok(authService.updateProfile(userId, dto));
    }
    
    
    // ---------------- CHANGE PASSWORD ----------------
    @PutMapping("/change-password/{userId}")
    public ResponseEntity<String> changePassword(
            @PathVariable Integer userId,
            @RequestBody ChangePasswordDto dto) {

        authService.changePassword(userId, dto);

        return ResponseEntity.ok("Password updated successfully");
    }
    
}
