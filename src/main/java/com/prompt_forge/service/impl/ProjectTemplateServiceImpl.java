package com.prompt_forge.service.impl;

import com.prompt_forge.entity.Project;
import com.prompt_forge.entity.ProjectFile;
import com.prompt_forge.error.ResourceNotFoundException;
import com.prompt_forge.reposityory.ProjectFileRepository;
import com.prompt_forge.reposityory.ProjectRepository;
import com.prompt_forge.service.ProjectTemplateService;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class ProjectTemplateServiceImpl implements ProjectTemplateService {

    private static final String TEMPLATE_BUCKET = "project-starter-templates";
    private static final String TARGET_BUCKET = "projects";
    private static final String TEMPLATE_NAME = "react-vite-tailwind-daisyui-starter";

    private final MinioClient minioClient;

    private final ProjectFileRepository projectFileRepository;
    private final ProjectRepository projectRepository;

    @Override
    public void initializeProjectTemplate(Long projectId) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("project", projectId.toString()));

        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(TEMPLATE_BUCKET)
                            .prefix(TEMPLATE_NAME + "/")
                            .recursive(true)
                            .build()
            );

            List<ProjectFile> filesToSave = new ArrayList<>();

            for (Result<Item> result : results) {
                Item item = result.get();
                String sourceKey = item.objectName();

                String cleanPath = sourceKey.replaceFirst(TEMPLATE_NAME + "/", "");
                String destinationKey = projectId + "/" + cleanPath;

                minioClient.copyObject(
                        CopyObjectArgs.builder()
                                .bucket(TARGET_BUCKET)
                                .object(destinationKey)
                                .source(
                                        CopySource.builder()
                                                .bucket(TEMPLATE_BUCKET)
                                                .object(sourceKey)
                                                .build()
                                )
                                .build()
                );

                // files metadata
                ProjectFile projectFile = ProjectFile.builder()
                        .project(project)
                        .path(cleanPath)
                        .minioObjectKey(destinationKey)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();

                filesToSave.add(projectFile);
            }

            projectFileRepository.saveAll(filesToSave);

        } catch (Exception e) {
            log.error("Error initializing project template for projectId {}: {}", projectId, e.getMessage(), e);
            throw new RuntimeException("Failed to initialize project template for projectId " + projectId, e);
        }
    }
}
