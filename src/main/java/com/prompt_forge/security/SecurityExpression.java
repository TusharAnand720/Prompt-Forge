package com.prompt_forge.security;

import com.prompt_forge.enums.Permission;
import com.prompt_forge.reposityory.ProjectMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component("security")
@RequiredArgsConstructor
@Slf4j
public class SecurityExpression {

    private final ProjectMemberRepository projectMemberRepository;
    private final AuthUtil authUtil;

    private boolean hasPermission(Long projectId, Permission projectPermission) {
        Long userId = authUtil.getCurrentUserId();
        return projectMemberRepository.findRoleByProjectIdAndUserId(projectId, userId)
                .map(role -> role.getPermissions().contains(projectPermission))
                .orElse(false);

    }

    public boolean canViewProject(Long projectId) {
        return hasPermission(projectId, Permission.VIEW);
    }

    public boolean canEditProject(Long projectId) {
        return hasPermission(projectId, Permission.EDIT);
    }

    public boolean canDeleteProject(Long projectId) {
        return hasPermission(projectId, Permission.DELETE);
    }

    public boolean canViewMembers(Long projectId) {
        return hasPermission(projectId, Permission.VIEW_MEMBERS);

    }

    public boolean canManageMembers(Long projectId) {
        return hasPermission(projectId, Permission.MANAGE_MEMBERS);
    }

}
