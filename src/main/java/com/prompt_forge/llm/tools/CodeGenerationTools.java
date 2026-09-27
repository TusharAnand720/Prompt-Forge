package com.prompt_forge.llm.tools;

import com.prompt_forge.service.ProjectFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class CodeGenerationTools {

    private final ProjectFileService projectFileService;

    private final Long projectId;

    @Tool(
            name = "read_files",
            description = "Reads the content of files in the project. " +
                    "Only Provide a list of file paths present inside the FILE_TREE to read. " +
                    "DO NOT input any other file paths. " +
                    "The output will be a list of strings, each containing the content of the corresponding file."
    )
    public List<String> readFiles(
            @ToolParam(description = "List of relative paths (example: ['src/App.tsx])")
            List<String> filePaths) {

        List<String> result = new ArrayList<>();

        for (String path : filePaths) {
            String cleanPath = path.startsWith("/") ? path.substring(1) : path;
            log.info("Reading file content for projectId: {}, path: {}", projectId, cleanPath);

            String content = projectFileService.getFileContent(projectId, cleanPath).content();

            result.add(String.format(
                    "----- Start of File : %s ----- \n%s\n ----- End of File -----",
                    cleanPath,
                    content)
            );
        }
        return result;
    }
}
