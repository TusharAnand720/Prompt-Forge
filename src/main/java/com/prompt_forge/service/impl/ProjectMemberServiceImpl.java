package com.prompt_forge.service.impl;

import com.prompt_forge.dto.member.InviteMemberRequest;
import com.prompt_forge.dto.member.MemberResponse;
import com.prompt_forge.dto.member.UpdateMemberRoleRequest;
import com.prompt_forge.entity.Project;
import com.prompt_forge.entity.ProjectMember;
import com.prompt_forge.entity.ProjectMemberId;
import com.prompt_forge.entity.User;
import com.prompt_forge.error.ResourceNotFoundException;
import com.prompt_forge.mapper.ProjectMemberMapper;
import com.prompt_forge.reposityory.ProjectMemberRepository;
import com.prompt_forge.reposityory.ProjectRepository;
import com.prompt_forge.reposityory.UserRepository;
import com.prompt_forge.security.AuthUtil;
import com.prompt_forge.service.ProjectMemberService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Transactional
public class ProjectMemberServiceImpl implements ProjectMemberService {

    ProjectMemberRepository projectMemberRepository;
    ProjectRepository projectRepository;
    UserRepository userRepository;

    ProjectMemberMapper projectMemberMapper;

    AuthUtil authUtil;

    // ==== GET PROJECT MEMBER ====
    @Override
    @PreAuthorize("@security.canViewMembers(#projectId)")
    public List<MemberResponse> getProjectMembers(Long projectId) {
        Long userId = authUtil.getCurrentUserId();
        return projectMemberRepository.findByProjectId(projectId).stream()
                .map(projectMemberMapper::toProjectMemberResponseFromMember)
                .toList();
    }


    // ==== INVITE PROJECT MEMBER ====
    @Override
    @PreAuthorize("@security.canManageMembers(#projectId)")
    public MemberResponse inviteMember(Long projectId, InviteMemberRequest request) {

        Long userId = authUtil.getCurrentUserId();

        Project project = getAccessibleProjectById(projectId, userId);

        User invitee = userRepository.findByUsername(request.username()).orElseThrow();
        if (invitee.getId().equals(userId)) {
            throw new RuntimeException("Cannot invite yourself");
        }

        ProjectMemberId projectMemberId = new ProjectMemberId(projectId, invitee.getId());
        if (projectMemberRepository.existsById(projectMemberId)) {
            throw new RuntimeException("Connot invite once again");
        }

        ProjectMember projectMember = ProjectMember.builder()
                .id(projectMemberId)
                .project(project)
                .user(invitee)
                .projectRole(request.role())
                .invitedAt(Instant.now())
                .build();
        projectMemberRepository.save(projectMember);

        return projectMemberMapper.toProjectMemberResponseFromMember(projectMember);
    }

    // ==== UPDATE PROJECT MEMBER ====
    @Override
    @PreAuthorize("@security.canManageMembers(#projectId)")
    public MemberResponse updateMemberRole(Long projectId, Long memberId, UpdateMemberRoleRequest request) {

        Long userId = authUtil.getCurrentUserId();

        ProjectMemberId projectMemberId = new ProjectMemberId(projectId, memberId);
        ProjectMember projectMember = projectMemberRepository.findById(projectMemberId).orElseThrow();
        projectMember.setProjectRole(request.role());

        projectMember = projectMemberRepository.save(projectMember);

        return projectMemberMapper.toProjectMemberResponseFromMember(projectMember);
    }

    // ==== DELETE PROJECT MEMBER ====
    @Override
    @PreAuthorize("@security.canManageMembers(#projectId)")
    public void removeProjectMember(Long projectId, Long memberId) {
        Long userId = authUtil.getCurrentUserId();
        ProjectMemberId projectMemberId = new ProjectMemberId(projectId, memberId);

        if (projectMemberRepository.existsById(projectMemberId)) {
            throw new RuntimeException("Member not found");
        }
        projectMemberRepository.deleteById(projectMemberId);
    }

    // === Internal Functions ===

    public Project getAccessibleProjectById(Long projectId, Long userId) {
        return projectRepository.findAccessibleProjectById(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("projectId-userId", projectId + "-" + userId));
    }
}
