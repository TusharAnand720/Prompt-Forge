package com.prompt_forge.service;

import com.prompt_forge.dto.project.FileContentResponse;
import com.prompt_forge.dto.project.FileNode;

import java.util.List;

public interface ProjectFileService {

    List<FileNode> getFileTree(Long projectId);

    FileContentResponse getFileContent(Long projectId, String path);

    void saveFile(Long projectId, String filePath, String fileContent);
}
