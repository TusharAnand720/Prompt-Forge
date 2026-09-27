package com.prompt_forge.dto.member;

import com.prompt_forge.enums.Role;

import java.time.Instant;

public record MemberResponse(
        Long userId,
        String username,
        String name,
        String avatarUrl,
        Role projectRole,
        Instant invitedAt) {

}
