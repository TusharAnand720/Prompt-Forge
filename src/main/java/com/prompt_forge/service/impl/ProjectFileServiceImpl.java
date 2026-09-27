package com.prompt_forge.service.impl;

import com.prompt_forge.dto.project.FileContentResponse;
import com.prompt_forge.dto.project.FileNode;
import com.prompt_forge.entity.Project;
import com.prompt_forge.entity.ProjectFile;
import com.prompt_forge.error.ResourceNotFoundException;
import com.prompt_forge.mapper.ProjectFileMapper;
import com.prompt_forge.reposityory.ProjectFileRepository;
import com.prompt_forge.reposityory.ProjectRepository;
import com.prompt_forge.service.ProjectFileService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectFileServiceImpl implements ProjectFileService {

    private static final String BUCKET_NAME = "projects";

    private final ProjectRepository projectRepository;
    private final ProjectFileRepository projectFileRepository;

    private final MinioClient minioClient;

    private final ProjectFileMapper projectFileMapper;

    @Value("${minio.project-bucket}")
    String projectBucket;

    @Override
    public List<FileNode> getFileTree(Long projectId) {
        List<ProjectFile> projectFileList = projectFileRepository.findByProjectId(projectId);
        return projectFileMapper.toListOfFileNode(projectFileList);
    }

    @Override
    public FileContentResponse getFileContent(Long projectId, String path) {
        String objectName = projectId + "/" + path;
        try {
            InputStream is = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(objectName)
                            .build());

            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return new FileContentResponse(path, content);

        } catch (Exception e) {
            log.error("Failed to get file content for projectId: {}, path: {}", projectId, path, e);
            throw new RuntimeException("Failed to get file content", e);
        }
    }

    @Override
    public void saveFile(Long projectId, String filePath, String fileContent) {

        log.info("Saving file for projectId: {}, path: {}", projectId, filePath);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("project", projectId.toString()));

        String cleanPath = filePath.startsWith("/") ? filePath.substring(1) : filePath;
        String objectKey = projectId + "/" + cleanPath;

        try {
            byte[] contentBytes = fileContent.getBytes(StandardCharsets.UTF_8);
            InputStream inputStream = new ByteArrayInputStream(contentBytes);

            // Saving file content into MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(projectBucket)
                            .object(objectKey)
                            .stream(inputStream, contentBytes.length, -1)
                            .contentType(determineContentType(cleanPath))
                            .build());

            // Saving File metadata into DB
            ProjectFile projectFile = projectFileRepository.findByProjectIdAndPath(projectId, cleanPath)
                    .orElseGet(() -> ProjectFile.builder()
                            .project(project)
                            .path(cleanPath)
                            .minioObjectKey(objectKey)
                            .createdAt(Instant.now())
                            .build());

            projectFile.setUpdatedAt(Instant.now());
            projectFileRepository.save(projectFile);
        } catch (Exception e) {
            log.error("Failed to save file {}/{}", projectId, cleanPath);
            throw new RuntimeException("File save failed", e);
        }
    }

    private String determineContentType(String path) {
        String type = URLConnection.guessContentTypeFromName(path);
        if (type != null) {
            return type;
        }
        if (path.endsWith(".jsx") || path.endsWith(".ts") || path.endsWith(".tsx")) {
            return "text/javascript";
        }
        if (path.endsWith(".json")) {
            return "application/json";
        }
        if (path.endsWith(".css")) {
            return "text/css";
        }
        return "text/plain";
    }

}
