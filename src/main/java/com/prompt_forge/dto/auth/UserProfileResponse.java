package com.prompt_forge.dto.auth;

public record UserProfileResponse(
        Long id,
        String username,
        String name) {
}
