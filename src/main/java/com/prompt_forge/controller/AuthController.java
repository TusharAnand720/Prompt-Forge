package com.prompt_forge.controller;

import com.prompt_forge.dto.auth.AuthResponse;
import com.prompt_forge.dto.auth.LoginRequest;
import com.prompt_forge.dto.auth.SignUpRequest;
import com.prompt_forge.dto.auth.UserProfileResponse;
import com.prompt_forge.service.AuthService;
import com.prompt_forge.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/auth")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class AuthController {

    AuthService authService;
    UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signUp(@RequestBody @Valid SignUpRequest signUpRequest) {
        return ResponseEntity.ok(authService.signup(signUpRequest));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getProfile() {
        long userId = 0L;
        return ResponseEntity.ok(userService.getProfile(userId));
    }
}
