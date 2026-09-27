package com.prompt_forge.mapper;

import com.prompt_forge.dto.project.ProjectResponse;
import com.prompt_forge.dto.project.ProjectSummaryResponse;
import com.prompt_forge.entity.Project;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    ProjectResponse toProjectResponse(Project project);

    ProjectSummaryResponse toProjectSummaryResponse(Project project);
}
