package com.prompt_forge.service;

import com.prompt_forge.dto.auth.UserProfileResponse;

public interface UserService {
    UserProfileResponse getProfile(Long userId);
}
