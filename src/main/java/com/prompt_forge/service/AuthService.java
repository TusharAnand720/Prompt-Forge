package com.prompt_forge.service;

import com.prompt_forge.dto.auth.AuthResponse;
import com.prompt_forge.dto.auth.LoginRequest;
import com.prompt_forge.dto.auth.SignUpRequest;

public interface AuthService {

    AuthResponse signup(SignUpRequest signUpRequest);

    AuthResponse login(LoginRequest loginRequest);
}
