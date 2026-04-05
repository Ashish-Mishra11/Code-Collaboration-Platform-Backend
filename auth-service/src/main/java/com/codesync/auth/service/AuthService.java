package com.codesync.auth.service;

import com.codesync.auth.dto.ChangePasswordDto;
import com.codesync.auth.dto.RegisterResponseDto;
import com.codesync.auth.dto.RegisterUserDto;
import com.codesync.auth.dto.UpdateUserProfileDto;
import com.codesync.auth.dto.UserProfileDto;
import com.codesync.auth.entity.User;
import java.util.List;

import org.jspecify.annotations.Nullable;

public interface AuthService {


	RegisterResponseDto register(RegisterUserDto registerUserDto);

    String login(String userName, String password);

    void logout(String token);

    String refreshToken(String token);


    UserProfileDto getUserById(Integer userId);

    UserProfileDto updateProfile(Integer userId, UpdateUserProfileDto dto);

    void changePassword(Integer userId, ChangePasswordDto dto);

    List<User> searchUsers(String username);


	
	     
}
