package com.codesync.auth.component;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.codesync.auth.dto.ChangePasswordDto;
import com.codesync.auth.dto.LoginUserDto;
import com.codesync.auth.dto.RegisterDeveloperDto;
import com.codesync.auth.dto.RegisterResponseDto;
import com.codesync.auth.dto.RegisterUserDto;
import com.codesync.auth.dto.UpdateUserProfileDto;
import com.codesync.auth.dto.UserProfileDto;
import com.codesync.auth.entity.PasswordResetToken;
import com.codesync.auth.entity.User;
import com.codesync.auth.repository.UserRepository;
import com.codesync.auth.service.AuthService;
import com.codesync.auth.service.implementation.DeveloperRoleService;
import com.codesync.auth.service.implementation.PasswordResetService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthResource {

    private final UserRepository userRepository;

	private AuthService authService;
	private DeveloperRoleService developerRoleService;
	private final PasswordResetService passwordResetService;
	
	public AuthResource(AuthService authService ,PasswordResetService passwordResetService, UserRepository userRepository,DeveloperRoleService developerRoleService) {
		this.authService=authService;
		this.passwordResetService=passwordResetService;
		this.userRepository = userRepository;
		this.developerRoleService=developerRoleService;
	}
	
	
	//------------------REGISTER for developer-------
	@PostMapping("/register/dev")
	public ResponseEntity<String> registerDeveloper(@ModelAttribute RegisterDeveloperDto registerDeveloperDto){
		System.out.println("inside the register developer controllers initial step");
		return ResponseEntity.ok(developerRoleService.registerDeveloperApplications(registerDeveloperDto));
	}
	
	//Approval for developer application via mail from admin to developer
	@PostMapping("/admin/approve")
	public ResponseEntity<String> SendMailToApprovedDeveloper(@RequestParam String email){
		return ResponseEntity.ok(developerRoleService.generateMailToDeveloper(email));
	}
	//rejection from admin via mail
	@PostMapping("/admin/reject")
	public ResponseEntity<String> deleteDeveloperDetails(@RequestParam String email){
		return ResponseEntity.ok(developerRoleService.rejectHandler(email));
	}
	
	//Approved developer registering themselves into the codesync 
	@PostMapping("/approvedeveloper")
	public ResponseEntity<RegisterResponseDto> registerApprovedDeveloper(@RequestParam String email, @RequestBody RegisterUserDto registerUserDto){
		return ResponseEntity.ok(authService.register(registerUserDto));
		
	}
    // ---------------- REGISTER ----------------
	// for normal user
    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDto> register(@Valid @RequestBody RegisterUserDto registerUserDto) {
    	return ResponseEntity.ok(authService.register(registerUserDto));
    	
    }
    // ---------------- LOGIN ----------------
    // for normal developer
    @PostMapping("/login")
    public ResponseEntity<String> login( @Valid @RequestBody LoginUserDto loginUserDto) {
    	System.out.println("Logining taking place");
    	return ResponseEntity.ok(authService.login(loginUserDto.getUserName(),loginUserDto.getPassword()));
    	
    }
 // ---------------- GET USER ID BY USERNAME (NEW - For Microservices) ----------------
    @GetMapping("/users/by-username")
    public ResponseEntity<Long> getUserIdByUsername(@RequestParam("username") String username) {
       
    	System.out.println("insdie ----------------------------------------");
    	Integer userId = authService.getUserIdByUsername(username);
        
        if (userId == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(userId.longValue());
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
    
 // ---------------- FORGOT PASSWORD ----------------
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody Map<String, String> body) {
        passwordResetService.forgotPassword(body.get("email"));
        return ResponseEntity.ok("If that email exists, a reset link has been sent");
    }

    // ---------------- RESET PASSWORD ----------------
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @RequestParam String token,
            @RequestBody Map<String, String> body) {

        String newPassword = body.get("password");
        if (newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body("Password cannot be empty");
        }

        PasswordResetToken resetToken = passwordResetService.validateAndFetch(token);

        User user = resetToken.getUser();
        // Encode and persist the new password
        user.setPasswordHash(new BCryptPasswordEncoder(12).encode(newPassword));
        userRepository.save(user); // ← was missing: without this, the change was lost

        passwordResetService.deleteToken(resetToken); // consume the token
        return ResponseEntity.ok("Password updated successfully");
    }
    
}
