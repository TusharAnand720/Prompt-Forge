package com.prompt_forge.dto.member;

import com.prompt_forge.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InviteMemberRequest(
        @Email
        @NotBlank
        String username,

        @NotNull
        Role role) {
}
