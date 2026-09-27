package com.prompt_forge.service.impl;

import com.prompt_forge.dto.auth.AuthResponse;
import com.prompt_forge.dto.auth.LoginRequest;
import com.prompt_forge.dto.auth.SignUpRequest;
import com.prompt_forge.entity.User;
import com.prompt_forge.error.BadRequestException;
import com.prompt_forge.mapper.UserMapper;
import com.prompt_forge.reposityory.UserRepository;
import com.prompt_forge.security.AuthUtil;
import com.prompt_forge.service.AuthService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Transactional
public class AuthServiceImpl implements AuthService {

    UserRepository userRepository;

    UserMapper userMapper;

    PasswordEncoder passwordEncoder;
    AuthUtil authUtil;
    AuthenticationManager authenticationManager;


    @Override
    public AuthResponse signup(SignUpRequest signUpRequest) {
        userRepository.findByUsername(signUpRequest.username()).ifPresent(user -> {
            throw new BadRequestException("User already exits with username: " + signUpRequest.username());
        });

        User user = userMapper.toEntityFromSignUpRequest(signUpRequest);
        user.setPassword(passwordEncoder.encode(signUpRequest.password()));
        user = userRepository.save(user);

        return new AuthResponse(authUtil.generateAccessToken(user), userMapper.toUserProfileResponse(user));
    }

    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password())
        );
        User user = (User) authentication.getPrincipal();

        return new AuthResponse(authUtil.generateAccessToken(user),
                userMapper.toUserProfileResponse(user));

    }
}
