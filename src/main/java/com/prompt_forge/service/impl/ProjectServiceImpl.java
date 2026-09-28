package com.prompt_forge.service.impl;

import com.prompt_forge.dto.project.ProjectRequest;
import com.prompt_forge.dto.project.ProjectResponse;
import com.prompt_forge.dto.project.ProjectSummaryResponse;
import com.prompt_forge.entity.Project;
import com.prompt_forge.entity.ProjectMember;
import com.prompt_forge.entity.ProjectMemberId;
import com.prompt_forge.entity.User;
import com.prompt_forge.enums.Role;
import com.prompt_forge.error.ResourceNotFoundException;
import com.prompt_forge.mapper.ProjectMapper;
import com.prompt_forge.reposityory.ProjectMemberRepository;
import com.prompt_forge.reposityory.ProjectRepository;
import com.prompt_forge.reposityory.UserRepository;
import com.prompt_forge.security.AuthUtil;
import com.prompt_forge.service.ProjectService;
import com.prompt_forge.service.ProjectTemplateService;
import com.prompt_forge.service.SubscriptionService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Transactional
public class ProjectServiceImpl implements ProjectService {

    ProjectRepository projectRepository;
    UserRepository userRepository;
    ProjectMemberRepository projectMemberRepository;

    ProjectMapper projectMapper;

    AuthUtil authUtil;

    SubscriptionService subscriptionService;
    ProjectTemplateService projectTemplateService;

    @Override
    public List<ProjectSummaryResponse> getUserProjects() {
        Long userId = authUtil.getCurrentUserId();
        Set<List<Integer>> uniquePairs = new HashSet<>();
        return projectRepository.findAllAccessibleByUser(userId).stream()
                .map(projectMapper::toProjectSummaryResponse)
                .toList();
    }

    @Override
    @PreAuthorize("@security.canViewProject(#projectId)")
    public ProjectResponse getUserProjectById(Long projectId) {
        Long userId = authUtil.getCurrentUserId();
        Project project = getAccessibleProjectById(projectId, userId);
        return projectMapper.toProjectResponse(project);
    }

    @Override
    public ProjectResponse createProject(ProjectRequest projectRequest) {

//        if (!subscriptionService.canCreateProject()) {
//            throw new BadRequestException("User has reached the maximum number of projects allowed by their subscription plan.");
//        }

        Long userId = authUtil.getCurrentUserId();

        User owner = userRepository.getReferenceById(userId);

        Project project = Project.builder()
                .name(projectRequest.name())
                .build();
        project = projectRepository.save(project);

        ProjectMemberId projectMemberId = new ProjectMemberId(project.getId(), owner.getId());
        ProjectMember projectMember = ProjectMember.builder()
                .id(projectMemberId)
                .projectRole(Role.OWNER)
                .user(owner)
                .project(project)
                .acceptedAt(Instant.now())
                .invitedAt(Instant.now())
                .build();
        projectMemberRepository.save(projectMember);

        projectTemplateService.initializeProjectTemplate(project.getId());

        return projectMapper.toProjectResponse(project);
    }

    @Override
    @PreAuthorize("@security.canEditProject(#projectId)")
    public ProjectResponse updateProject(Long projectId, ProjectRequest projectRequest) {
        Long userId = authUtil.getCurrentUserId();
        Project project = getAccessibleProjectById(projectId, userId);
        project.setName(projectRequest.name());
        project = projectRepository.save(project);
        return projectMapper.toProjectResponse(project);
    }

    @Override
    @PreAuthorize("@security.canDeleteProject(#projectId)")
    public void deleteProject(Long projectId) {
        Long userId = authUtil.getCurrentUserId();
        Project project = getAccessibleProjectById(projectId, userId);
        project.setIsActive(false);
        project.setDeletedAt(Instant.now());
        projectRepository.save(project);
    }

    // === Internal Functions ===

    public Project getAccessibleProjectById(Long projectId, Long userId) {
        return projectRepository.findAccessibleProjectById(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("projectId-userId", projectId + "-" + userId));
    }
}
