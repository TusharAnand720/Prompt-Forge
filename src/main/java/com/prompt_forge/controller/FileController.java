package com.prompt_forge.controller;

import com.prompt_forge.dto.project.FileContentResponse;
import com.prompt_forge.dto.project.FileNode;
import com.prompt_forge.service.ProjectFileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/projects/{projectId}/files")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class FileController {

    ProjectFileService fileService;

    @GetMapping("/{id}")
    public ResponseEntity<List<FileNode>> getFileTree(@PathVariable Long projectId) {
        return ResponseEntity.ok(fileService.getFileTree(projectId));
    }

    @GetMapping("{*path}")
    public ResponseEntity<FileContentResponse> getFile(@PathVariable Long projectId, @PathVariable String path) {
        Long userId = 0L;
        return ResponseEntity.ok(fileService.getFileContent(projectId, path));
    }
}
