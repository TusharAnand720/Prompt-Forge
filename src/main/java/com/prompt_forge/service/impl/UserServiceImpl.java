package com.prompt_forge.service.impl;

import com.prompt_forge.dto.auth.UserProfileResponse;
import com.prompt_forge.entity.User;
import com.prompt_forge.error.ResourceNotFoundException;
import com.prompt_forge.mapper.UserMapper;
import com.prompt_forge.reposityory.UserRepository;
import com.prompt_forge.security.AuthUtil;
import com.prompt_forge.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class UserServiceImpl implements UserService, UserDetailsService {

    UserRepository userRepository;

    AuthUtil authUtil;

    UserMapper userMapper;
    
    @Override
    public UserProfileResponse getProfile() {
        Long userId = authUtil.getCurrentUserId();
        Optional<User> userOptional = userRepository.findById(userId);
        return userOptional.map(userMapper::toUserProfileResponse)
                .orElseThrow(() -> new ResourceNotFoundException("user", userId.toString()));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("user", username));
    }
}
