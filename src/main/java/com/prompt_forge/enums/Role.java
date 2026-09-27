package com.prompt_forge.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

import static com.prompt_forge.enums.Permission.*;

@RequiredArgsConstructor
@Getter
public enum Role {
    OWNER(EDIT, VIEW, DELETE, VIEW_MEMBERS, MANAGE_MEMBERS),
    EDITOR(EDIT, VIEW, DELETE, VIEW_MEMBERS),
    VIEWER(VIEW, VIEW_MEMBERS);

    private final Set<Permission> permissions;

    Role(Permission... permissions) {
        this.permissions = Set.of(permissions);
    }
}
