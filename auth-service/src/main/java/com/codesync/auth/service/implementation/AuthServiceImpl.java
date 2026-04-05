package com.codesync.auth.service.implementation;

import java.security.PrivateKey;
import java.time.LocalDateTime;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.codesync.auth.dto.ChangePasswordDto;
import com.codesync.auth.dto.RegisterResponseDto;
import com.codesync.auth.dto.RegisterUserDto;
import com.codesync.auth.dto.UpdateUserProfileDto;
import com.codesync.auth.dto.UserProfileDto;
import com.codesync.auth.entity.User;
import com.codesync.auth.exceptionhandler.EmailAlreadyExistsException;
import com.codesync.auth.exceptionhandler.UserNotFoundException;
import com.codesync.auth.exceptionhandler.UsernameAlreadyExistsException;
import com.codesync.auth.repository.UserRepository;
import com.codesync.auth.service.AuthService;

@Service
public class AuthServiceImpl implements AuthService {
	
	@Autowired
	private UserRepository userRepository;
	

    private BCryptPasswordEncoder encoder=new BCryptPasswordEncoder(12);
    
	@Autowired
	AuthenticationManager authenticationManager;
	
	@Autowired
	private JwtService jwtService;
	
	@Autowired
	private ModelMapper modelMapper;
	

	//Register
	@Override
	public RegisterResponseDto register( RegisterUserDto registerUserDto) {
		// TODO Auto-generated method stub
	    // 1. Check if email already exists
	    if (userRepository.existsByEmail(registerUserDto.getEmail())) {
	        throw new EmailAlreadyExistsException("Email already exists");
	    }
	    
	    // 2. Check if username already exists
	    if (userRepository.existsByUserName(registerUserDto.getUserName())) {
	        throw new UsernameAlreadyExistsException("Username already exists");
	    }
	    
	    // 3. Convert DTO → Entity
	    User user = new User();
	    user.setUserName(registerUserDto.getUserName());
	    user.setEmail(registerUserDto.getEmail());
	    user.setFullName(registerUserDto.getFullName());
	    
	    // 4. Hash password
	    user.setPasswordHash(encoder.encode(registerUserDto.getPassword()));
	    
	    // 5. Set system-controlled fields
	    user.setRole("DEVELOPER");     // default role
	    user.setProvider("LOCAL");
	    user.setIsActive(true);
	    user.setCreateAt(LocalDateTime.now());

	    // 6. Save user
	    User savedUser = userRepository.save(user);
	    
	    // 7. Convert Entity → Response DTO
	    RegisterResponseDto response = new RegisterResponseDto();
	    response.setUserId(savedUser.getUserId());
	    response.setUserName(savedUser.getUserName());
	    response.setEmail(savedUser.getEmail());
	    response.setRole(savedUser.getRole());

	    return response;
	}

	//Login
	@Override
	public String login(String userName, String password) {
		// TODO Auto-generated method stub
		Authentication authentication = authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(userName, password));

		if(authentication.isAuthenticated())
			return jwtService.generateToken(userName);
		else
			return "Login Failed";
		
	
	}

	//Logout
	//it is kept if we have to blackList some user then we can store the token in db and then do the coding to remove them here
	@Override
	public void logout(String token) {
		// TODO Auto-generated method stub
		
	}

	//refresh feature
	@Override
	public String refreshToken(String token) {

	    //  Remove "Bearer "
	    token = token.substring(7);

	    //  Extract userId (subject)
	    String userId = jwtService.extractUserName(token);

	    //  Validate token (important)
	    if (!jwtService.isTokenValid(token, userId)) {
	        throw new RuntimeException("Invalid or expired token");
	    }

	    //  Generate new token
	    return jwtService.generateToken(userId);
	}
	
	//Get profile
	@Override
	public UserProfileDto getUserById(Integer userId) {
		// TODO Auto-generated method stub
		 User user = userRepository.findByUserId(userId).orElseThrow(()->new UserNotFoundException("User not found of this email"));
		 UserProfileDto map = modelMapper.map(user,UserProfileDto.class);
		 return map;
	}

	//update profile
	@Override
	public UserProfileDto updateProfile(Integer userId, UpdateUserProfileDto dto) {

	    User user = userRepository.findById(userId)
	            .orElseThrow(() -> new UserNotFoundException("User not found of this userID"));

	   
	    if (dto.getFullName() != null) {
	        user.setFullName(dto.getFullName());
	    }

	    if (dto.getAvatarUrl() != null) {
	        user.setAvatarUrl(dto.getAvatarUrl());
	    }

	    if (dto.getBio() != null) {
	        user.setBio(dto.getBio());
	    }

	    userRepository.save(user);

	    return modelMapper.map(user,UserProfileDto.class);
	}

	//change password
	@Override
	public void changePassword(Integer userId, ChangePasswordDto dto) {

	    User user = userRepository.findById(userId)
	            .orElseThrow(() -> new RuntimeException("User not found"));

	    //Check old password
	    if (!encoder.matches(dto.getOldPassword(), user.getPasswordHash())) {
	        throw new RuntimeException("Old password is incorrect");
	    }

	    //Encode new password
	    String encodedPassword = encoder.encode(dto.getNewPassword());

	    user.setPasswordHash(encodedPassword);

	    userRepository.save(user);
	}
	
	@Override
	public List<User> searchUsers(String username) {
		// TODO Auto-generated method stub
		return null;
	}

}
