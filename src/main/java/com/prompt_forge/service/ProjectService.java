package com.prompt_forge.service;

import com.prompt_forge.dto.project.ProjectRequest;
import com.prompt_forge.dto.project.ProjectResponse;
import com.prompt_forge.dto.project.ProjectSummaryResponse;

import java.util.List;

public interface ProjectService {
    List<ProjectSummaryResponse> getUserProjects();

    ProjectResponse getUserProjectById(Long id);

    ProjectResponse createProject(ProjectRequest projectRequest);

    ProjectResponse updateProject(Long id, ProjectRequest projectRequest);

    void deleteProject(Long id);
}
