package com.prompt_forge.dto.member;

import com.prompt_forge.enums.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
        @NotNull
        Role role) {
}
