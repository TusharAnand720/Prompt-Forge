package com.prompt_forge.service;

import com.prompt_forge.dto.deploy.DeployResponse;

public interface DeploymentService {
    DeployResponse deploy(Long projectId);
}
